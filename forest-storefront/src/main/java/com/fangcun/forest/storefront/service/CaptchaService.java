package com.fangcun.forest.storefront.service;

import jakarta.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private record Challenge(String answer, Instant expiresAt) implements Serializable { }

    public byte[] create(HttpSession session, String purpose) throws IOException {
        checkPurpose(purpose);
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < 4; i++) answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        BufferedImage image = new BufferedImage(144, 52, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(239, 244, 229)); g.fillRect(0, 0, 144, 52);
            for (int i = 0; i < 9; i++) {
                g.setColor(new Color(170 + random.nextInt(50), 190 + random.nextInt(40), 155 + random.nextInt(50)));
                g.drawLine(random.nextInt(144), random.nextInt(52), random.nextInt(144), random.nextInt(52));
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 29));
            for (int i = 0; i < 4; i++) {
                g.setColor(new Color(38 + random.nextInt(30), 70 + random.nextInt(40), 42 + random.nextInt(30)));
                g.drawString(answer.substring(i, i + 1), 13 + i * 32, 35 + random.nextInt(5));
            }
        } finally { g.dispose(); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        synchronized (session) {
            session.setAttribute("forest.captcha." + purpose, new Challenge(answer.toString(), Instant.now().plusSeconds(120)));
        }
        return output.toByteArray();
    }

    /** A challenge is consumed on every verification, including wrong answers. */
    public boolean verify(HttpSession session, String purpose, String answer) {
        checkPurpose(purpose);
        if (session == null) return false;
        synchronized (session) {
            Object value = session.getAttribute("forest.captcha." + purpose);
            session.removeAttribute("forest.captcha." + purpose);
            return value instanceof Challenge challenge && challenge.expiresAt().isAfter(Instant.now())
                    && answer != null && challenge.answer().equals(answer.strip().toUpperCase(Locale.ROOT));
        }
    }

    private void checkPurpose(String purpose) {
        if (!"login".equals(purpose) && !"register".equals(purpose)) throw new IllegalArgumentException("Invalid captcha purpose");
    }
}
