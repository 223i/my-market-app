package com.iron.mymarket.controller;

import com.iron.mymarket.model.Item;
import com.iron.mymarket.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller("/items")
public class ItemsController {

    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public String getItemPage(){
        return "item.html";
    }

    @GetMapping
    public String getItems(@RequestParam(value = "search", defaultValue = "") String search,
                           @RequestParam(value = "sort", defaultValue = "NO") String sort,
                           @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
                           @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                           Model model){
        //retrieve items
//        List<Item> items = itemService.findItems(search, sort, pageNumber, pageSize);
//        model.addAttribute("items", items);
        return "items";
    }

}
