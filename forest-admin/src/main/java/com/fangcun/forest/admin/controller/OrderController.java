package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.enums.OrderStatus;
import com.fangcun.forest.admin.service.OrderService;
import com.fangcun.forest.admin.service.UserService;
import com.fangcun.forest.admin.web.OrderEditForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orders;
    private final UserService users;
    public OrderController(OrderService orders, UserService users) { this.orders = orders; this.users = users; }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String orderNo, @RequestParam(required = false) Long userId,
            @RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "1") long page, Model model) {
        model.addAttribute("result", orders.search(orderNo, userId, status, new PageRequest(page, 10)));
        model.addAttribute("orderNo", orderNo.strip()); model.addAttribute("userId", userId);
        model.addAttribute("status", status); model.addAttribute("statuses", OrderStatus.values()); return "orders/list";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) { model.addAttribute("form", OrderEditForm.from(orders.get(id))); return data(id, model); }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id, @Valid @ModelAttribute("form") OrderEditForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { orders.update(id, form); redirect.addFlashAttribute("success", "收货信息已更新。"); return "redirect:/orders/" + id; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        return data(id, model);
    }
    @PostMapping("/{id}/ship")
    public String ship(@PathVariable long id, @RequestParam int version, RedirectAttributes redirect) {
        try { orders.ship(id, version); redirect.addFlashAttribute("success", "订单已发货。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/orders/" + id;
    }
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable long id, @RequestParam int version, RedirectAttributes redirect) {
        try { orders.cancel(id, version); redirect.addFlashAttribute("success", "订单已取消，库存已归还。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/orders/" + id;
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, @RequestParam int version, RedirectAttributes redirect) {
        try { orders.delete(id, version); redirect.addFlashAttribute("success", "已取消订单及明细已删除。"); return "redirect:/orders"; }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/orders/" + id; }
    }
    private String data(long id, Model model) {
        var order = orders.get(id); model.addAttribute("order", order); model.addAttribute("items", orders.items(id));
        model.addAttribute("customer", users.get(order.getUserId())); return "orders/detail";
    }
}
