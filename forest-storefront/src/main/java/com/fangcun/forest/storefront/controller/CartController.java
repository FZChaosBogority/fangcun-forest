package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }
    @GetMapping
    public String cart(@AuthenticationPrincipal CustomerDetails customer, Model model) {
        model.addAttribute("cart", carts.view(customer.getUserId()));
        return "cart/index";
    }
    @PostMapping("/add")
    public String add(@AuthenticationPrincipal CustomerDetails customer, @RequestParam long productId,
            @RequestParam(defaultValue = "1") int quantity, RedirectAttributes redirect) {
        try { carts.add(customer.getUserId(), productId, quantity); redirect.addFlashAttribute("success", "已把这份绿意加入购物车。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/cart";
    }
    @PostMapping("/{id}/update")
    public String update(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id,
            @RequestParam int quantity, @RequestParam int version, RedirectAttributes redirect) {
        try { carts.update(customer.getUserId(), id, quantity, version); redirect.addFlashAttribute("success", "数量已更新。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/cart";
    }
    @PostMapping("/{id}/remove")
    public String remove(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id,
            @RequestParam int version, RedirectAttributes redirect) {
        try { carts.remove(customer.getUserId(), id, version); redirect.addFlashAttribute("success", "已移除这份商品。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/cart";
    }
}
