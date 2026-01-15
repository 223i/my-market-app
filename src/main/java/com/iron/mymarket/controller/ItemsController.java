package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.service.ItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ItemsController {

    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping({"/", "/items"})
    public String getItems(@RequestParam(value = "search", defaultValue = "") String search,
                           @RequestParam(value = "sort", defaultValue = "NO") ItemSort sort,
                           @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
                           @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                           Model model){
        List<ItemDto> items = itemService.findItems(search, sort, pageNumber, pageSize);
        model.addAttribute("items", items);
        return "items";
    }

}
