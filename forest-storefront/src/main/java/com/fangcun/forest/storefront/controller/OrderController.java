package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.SalesOrder;
import com.fangcun.forest.storefront.enums.OrderStatus;
import com.fangcun.forest.storefront.security.CustomerDetails;
import com.fangcun.forest.storefront.service.OrderService;
import com.fangcun.forest.storefront.web.CheckoutDraft;
import com.fangcun.forest.storefront.web.CheckoutTokens;
import com.fangcun.forest.storefront.web.ShippingForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    private final OrderService orders;
    private final CheckoutTokens tokens;
    public OrderController(OrderService orders, CheckoutTokens tokens) { this.orders = orders; this.tokens = tokens; }

    @GetMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomerDetails customer,
            @RequestParam(required = false) List<Long> items, HttpSession session, Model model,
            RedirectAttributes redirect) {
        try {
            CheckoutDraft draft = orders.prepare(customer.getUserId(), items);
            model.addAttribute("draft", draft); model.addAttribute("token", tokens.store(session, draft));
            model.addAttribute("shipping", new ShippingForm());
            return "orders/checkout";
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/cart";
        }
    }

    @PostMapping("/checkout")
    public String place(@AuthenticationPrincipal CustomerDetails customer, @RequestParam String token,
            @Valid @ModelAttribute("shipping") ShippingForm shipping, BindingResult errors,
            HttpSession session, Model model, RedirectAttributes redirect) {
        CheckoutDraft draft;
        try { draft = tokens.require(session, token, customer.getUserId()); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/cart"; }
        if (errors.hasErrors()) {
            model.addAttribute("draft", draft); model.addAttribute("token", token); return "orders/checkout";
        }
        try {
            SalesOrder order = orders.place(customer.getUserId(), draft, shipping);
            return "redirect:/orders/" + order.getId();
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/cart";
        }
    }

    @GetMapping("/orders")
    public String list(@AuthenticationPrincipal CustomerDetails customer,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") long page, Model model) {
        model.addAttribute("result", orders.list(customer.getUserId(), status, new PageRequest(page, 8)));
        model.addAttribute("selectedStatus", status); model.addAttribute("statuses", OrderStatus.values());
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id, Model model) {
        model.addAttribute("order", orders.owned(customer.getUserId(), id));
        model.addAttribute("items", orders.items(customer.getUserId(), id));
        return "orders/detail";
    }

    @GetMapping("/orders/{id}/pay")
    public String payment(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id, Model model) {
        model.addAttribute("order", orders.owned(customer.getUserId(), id)); return "orders/payment";
    }

    @PostMapping("/orders/{id}/pay")
    public String pay(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes redirect) {
        if (!confirmed) { redirect.addFlashAttribute("error", "请先勾选确认模拟支付。"); return "redirect:/orders/" + id + "/pay"; }
        try { orders.pay(customer.getUserId(), id); redirect.addFlashAttribute("success", "模拟支付成功，无真实资金扣款。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id, RedirectAttributes redirect) {
        try { orders.cancel(customer.getUserId(), id); redirect.addFlashAttribute("success", "订单已取消，库存已归还。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/complete")
    public String complete(@AuthenticationPrincipal CustomerDetails customer, @PathVariable long id, RedirectAttributes redirect) {
        try { orders.complete(customer.getUserId(), id); redirect.addFlashAttribute("success", "已确认收货，愿绿意陪伴你的日常。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/orders/" + id;
    }
}
