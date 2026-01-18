package com.iron.mymarket.controller;

import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class OrdersController {

    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<OrderDto> orders = orderService.findOrders();
        model.addAttribute("orders", orders);
        return "orders";
    }

    //TODO: finalize new order=true case
    @GetMapping("/orders/{id}")
    public String getOrderById(@PathVariable Long id,
                               @RequestParam(required = false,
                                       value = "newOrder", defaultValue = "false") Boolean newOrder, Model model) {
        OrderDto order = orderService.findOrderById(id);
        model.addAttribute("order", order);
        return "order";
    }

}
