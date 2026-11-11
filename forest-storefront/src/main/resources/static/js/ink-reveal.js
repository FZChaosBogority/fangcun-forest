(() => {
  "use strict";

  const DURATION = 0.24;
  const STAGGER = 0.045;
  const FOCUS_END = 0.7;
  const MAX_RADIUS = 5; // Actual atlas texels, never CSS/backdrop blur pixels.
  const PADDING = MAX_RADIUS + 3;
  const ATLAS_WIDTH = 2048;
  const clamp = value => Math.max(0, Math.min(1, value));
  const smooth = value => value * value * (3 - 2 * value);
  const segmenter = typeof Intl.Segmenter === "function"
    ? new Intl.Segmenter("zh-CN", { granularity: "grapheme" }) : null;

  // An explicitly normalized, symmetric 5 x 5 Gaussian kernel, sigma = 1.
  const kernel = [];
  let weightSum = 0;
  for (let y = -2; y <= 2; y += 1) {
    for (let x = -2; x <= 2; x += 1) {
      const weight = Math.exp(-(x * x + y * y) / 2);
      kernel.push({ x, y, weight });
      weightSum += weight;
    }
  }
  kernel.forEach(sample => { sample.weight /= weightSum; });

  function makeCanvas(width = 1, height = 1) {
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    return canvas;
  }

  function characterRanges(source) {
    const walker = document.createTreeWalker(source, NodeFilter.SHOW_TEXT);
    const result = [];
    while (walker.nextNode()) {
      const node = walker.currentNode;
      const characters = segmenter
        ? Array.from(segmenter.segment(node.textContent), item => item.segment)
        : Array.from(node.textContent);
      let offset = 0;
      for (const character of characters) {
        if (!/^\s+$/u.test(character)) {
          const range = document.createRange();
          range.setStart(node, offset);
          range.setEnd(node, offset + character.length);
          if (range.getBoundingClientRect().width > 0) result.push({ character, range });
        }
        offset += character.length;
      }
    }
    return result;
  }

  function readColor(value) {
    const components = value.match(/[\d.]+/g)?.map(Number) || [255, 255, 255];
    return {
      rgb: `rgb(${components[0]}, ${components[1]}, ${components[2]})`,
      alpha: components.length > 3 ? components[3] : 1
    };
  }

  class GlyphInkReveal {
    constructor(root) {
      this.root = root;
      this.sources = Array.from(root.querySelectorAll("[data-ink-text]"));
      this.canvas = makeCanvas();
      this.canvas.className = "ink-glyph-canvas";
      this.canvas.setAttribute("aria-hidden", "true");
      this.canvas.style.visibility = "hidden";
      this.context = this.canvas.getContext("2d", { alpha: true });
      this.atlas = makeCanvas();
      this.atlasContext = this.atlas.getContext("2d", { alpha: true });
      this.scratch = makeCanvas();
      this.scratchContext = this.scratch.getContext("2d", { alpha: true });
      this.starts = new Map();
      this.entries = [];
      this.tiles = new Map();
      this.frameId = 0;
      this.rebuildId = 0;
      this.ready = false;
      this.failed = false;
      this.motion = matchMedia("(prefers-reduced-motion: reduce)");
      if (!this.context || !this.atlasContext || !this.scratchContext) return;
      root.append(this.canvas);

      root.addEventListener("forest:storychange", () => {
        // Changing a story replaces its real DOM text; rebuild only its timing.
        if (!this.ready || this.motion.matches || this.failed) return;
        this.starts.set("main", performance.now() / 1000);
        this.scheduleRebuild();
      });
      this.motion.addEventListener("change", () => {
        if (this.motion.matches) this.showNative("reduced-motion");
        else this.scheduleRebuild(true);
      });
      window.addEventListener("resize", () => this.scheduleRebuild());
      window.addEventListener("pageshow", event => {
        if (event.persisted) this.scheduleRebuild();
      });

      if (!this.motion.matches) root.classList.add("ink-preparing");
      const timeout = setTimeout(() => {
        if (!this.ready) this.fallback();
      }, 1500);
      (document.fonts?.ready || Promise.resolve()).then(() => {
        clearTimeout(timeout);
        if (this.failed) return;
        this.ready = true;
        this.rebuild(true);
        this.observer = new ResizeObserver(() => this.scheduleRebuild());
        this.observer.observe(root);
        document.fonts?.addEventListener("loadingdone", () => this.scheduleRebuild());
      }).catch(() => this.fallback());
    }

    scheduleRebuild(restart = false) {
      if (!this.ready || this.failed) return;
      this.restartRequested = this.restartRequested || restart;
      cancelAnimationFrame(this.rebuildId);
      this.rebuildId = requestAnimationFrame(() => {
        const reset = this.restartRequested;
        this.restartRequested = false;
        this.rebuild(reset);
      });
    }

    rebuild(restart = false) {
      cancelAnimationFrame(this.frameId);
      if (this.motion.matches) {
        this.showNative("reduced-motion");
        return;
      }
      try {
        // Briefly remove only the transparent-text rule to read original colours.
        // Everything below runs synchronously before the next browser paint.
        this.root.classList.remove("ink-rendering");
        const bounds = this.root.getBoundingClientRect();
        if (!bounds.width || !bounds.height) {
          this.showNative("hidden");
          return;
        }
        this.scale = Math.min(2, Math.max(1, window.devicePixelRatio || 1));
        this.width = bounds.width;
        this.height = bounds.height;
        this.canvas.width = Math.ceil(bounds.width * this.scale);
        this.canvas.height = Math.ceil(bounds.height * this.scale);
        this.context.setTransform(this.scale, 0, 0, this.scale, 0, 0);
        const atlasContext = this.atlasContext;
        const glyphs = new Map();
        const indices = new Map();
        this.entries = [];

        for (const source of this.sources) {
          const characters = characterRanges(source);
          if (!characters.length) continue;
          const style = getComputedStyle(source);
          const size = parseFloat(style.fontSize) * this.scale;
          const font = `${style.fontStyle} ${style.fontWeight} ${size}px ${style.fontFamily}`;
          const color = readColor(style.color);
          const group = source.dataset.inkText;

          // A zero-size inline probe obtains the browser's ACTUAL baseline.
          // It is removed immediately and is never animated or kept in layout.
          const last = characters[characters.length - 1].range;
          const probe = document.createElement("span");
          probe.setAttribute("aria-hidden", "true");
          probe.style.cssText = "display:inline-block;width:0;height:0;padding:0;margin:0;border:0;vertical-align:baseline";
          source.append(probe);
          const baselineOffset = probe.getBoundingClientRect().top - last.getBoundingClientRect().top;
          probe.remove();

          atlasContext.font = font;
          atlasContext.textBaseline = "alphabetic";
          atlasContext.textAlign = "left";
          for (const item of characters) {
            const character = item.character;
            const key = `${font}\u0000${character}`;
            let glyph = glyphs.get(key);
            if (!glyph) {
              const metrics = atlasContext.measureText(character);
              const left = metrics.actualBoundingBoxLeft;
              const ascent = metrics.actualBoundingBoxAscent;
              glyph = {
                key, character, font, left, ascent,
                width: Math.max(1, Math.ceil(left + metrics.actualBoundingBoxRight)) + 2 * PADDING,
                height: Math.max(1, Math.ceil(ascent + metrics.actualBoundingBoxDescent)) + 2 * PADDING
              };
              glyphs.set(key, glyph);
            }
            const rect = item.range.getBoundingClientRect();
            const index = indices.get(group) || 0;
            indices.set(group, index + 1);
            this.entries.push({
              glyph, color, group, index,
              // These coordinates remain fixed throughout the reveal.
              x: rect.left - bounds.left - (glyph.left + PADDING) / this.scale,
              y: rect.top - bounds.top + baselineOffset - (glyph.ascent + PADDING) / this.scale
            });
          }
        }

        let x = 0;
        let y = 0;
        let rowHeight = 0;
        let maxWidth = 1;
        let maxHeight = 1;
        for (const glyph of glyphs.values()) {
          if (x + glyph.width > ATLAS_WIDTH) { x = 0; y += rowHeight; rowHeight = 0; }
          glyph.x = x;
          glyph.y = y;
          x += glyph.width;
          rowHeight = Math.max(rowHeight, glyph.height);
          maxWidth = Math.max(maxWidth, glyph.width);
          maxHeight = Math.max(maxHeight, glyph.height);
        }
        this.atlas.width = ATLAS_WIDTH;
        this.atlas.height = Math.max(1, y + rowHeight);
        atlasContext.fillStyle = "#fff";
        atlasContext.textBaseline = "alphabetic";
        atlasContext.textAlign = "left";
        for (const glyph of glyphs.values()) {
          atlasContext.font = glyph.font;
          // White RGB with the real antialiased glyph coverage in alpha.
          atlasContext.fillText(glyph.character,
            glyph.x + PADDING + glyph.left, glyph.y + PADDING + glyph.ascent);
        }
        this.scratch.width = maxWidth;
        this.scratch.height = maxHeight;
        this.tiles.clear();
        for (const entry of this.entries) entry.sharp = this.sharpTile(entry);

        const now = performance.now() / 1000;
        for (const group of indices.keys()) {
          if (restart || !this.starts.has(group)) this.starts.set(group, now);
        }
        this.end = Math.max(now, ...this.entries.map(entry =>
          this.starts.get(entry.group) + entry.index * STAGGER + DURATION));
        this.root.classList.remove("ink-preparing");
        this.root.classList.add("ink-rendering");
        this.root.dataset.inkState = "running";
        this.canvas.style.visibility = "visible";
        this.root.dispatchEvent(new CustomEvent("forest:ink-ready", { detail: {
          duration: DURATION, stagger: STAGGER, focusEnd: FOCUS_END,
          maxRadius: MAX_RADIUS, kernelSize: 5, atlasScale: this.scale,
          glyphCount: this.entries.length
        } }));
        this.frame(performance.now());
      } catch (error) {
        this.fallback();
        console.warn("Ink reveal is unavailable; native text is preserved.", error);
      }
    }

    sharpTile(entry) {
      const { glyph, color } = entry;
      const key = `${glyph.key}\u0000${color.rgb}`;
      if (this.tiles.has(key)) return this.tiles.get(key);
      const tile = makeCanvas(glyph.width, glyph.height);
      const context = tile.getContext("2d");
      context.drawImage(this.atlas, glyph.x, glyph.y, glyph.width, glyph.height,
        0, 0, glyph.width, glyph.height);
      context.globalCompositeOperation = "source-in";
      context.fillStyle = color.rgb;
      context.fillRect(0, 0, glyph.width, glyph.height);
      this.tiles.set(key, tile);
      return tile;
    }

    sampleCoverage(entry, radius) {
      const { glyph, color } = entry;
      const context = this.scratchContext;
      context.clearRect(0, 0, this.scratch.width, this.scratch.height);
      context.globalCompositeOperation = "lighter";
      // Native canvas bilinear sampling accumulates exactly:
      // A'(p) = sum(w[x,y] * A(p + (x,y) * radius/2)), x,y in [-2,2].
      // Normalized additive alpha is a 25-tap coverage convolution, NOT a
      // moving DOM glyph. Only the transparent glyph atlas is sampled.
      for (const sample of kernel) {
        context.globalAlpha = sample.weight;
        context.drawImage(this.atlas, glyph.x, glyph.y, glyph.width, glyph.height,
          sample.x * radius / 2, sample.y * radius / 2, glyph.width, glyph.height);
      }
      context.globalAlpha = 1;
      context.globalCompositeOperation = "source-in";
      context.fillStyle = color.rgb;
      context.fillRect(0, 0, this.scratch.width, this.scratch.height);
      return this.scratch;
    }

    frame(milliseconds) {
      try {
        const now = milliseconds / 1000;
        if (now >= this.end) { this.showNative("complete"); return; }
        const context = this.context;
        context.clearRect(0, 0, this.width, this.height);
        for (const entry of this.entries) {
          const phase = clamp((now - this.starts.get(entry.group) - entry.index * STAGGER) / DURATION);
          if (phase <= 0) continue;
          const focus = clamp(phase / FOCUS_END);
          const radius = MAX_RADIUS * Math.pow(1 - focus, 3);
          const image = radius > 0.001 ? this.sampleCoverage(entry, radius) : entry.sharp;
          context.globalAlpha = smooth(phase) * entry.color.alpha;
          context.drawImage(image, 0, 0, entry.glyph.width, entry.glyph.height,
            entry.x, entry.y, entry.glyph.width / this.scale, entry.glyph.height / this.scale);
        }
        context.globalAlpha = 1;
        this.frameId = requestAnimationFrame(time => this.frame(time));
      } catch (error) {
        this.fallback();
        console.warn("Ink reveal stopped; native text is preserved.", error);
      }
    }

    showNative(state) {
      cancelAnimationFrame(this.frameId);
      this.context?.clearRect(0, 0, this.width || 1, this.height || 1);
      this.canvas.style.visibility = "hidden";
      this.root.classList.remove("ink-preparing", "ink-rendering");
      this.root.dataset.inkState = state;
    }

    fallback() {
      this.failed = true;
      this.showNative("native");
    }
  }

  document.querySelectorAll(".forest-story").forEach(root => new GlyphInkReveal(root));
})();
