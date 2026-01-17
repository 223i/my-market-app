package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.model.Paging;
import com.iron.mymarket.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
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
                           Model model) {
        Page<ItemDto> page = itemService.findItems(search, sort, pageNumber, pageSize);

        model.addAttribute("items", toRows(page.getContent(), 3));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        model.addAttribute("paging", new Paging(
                pageSize,
                pageNumber,
                page.hasPrevious(),
                page.hasNext()
        ));

        return "items";
    }

    private List<List<ItemDto>> toRows(List<ItemDto> items, int rowSize) {
        List<List<ItemDto>> rows = new ArrayList<>();

        for (int i = 0; i < items.size(); i += rowSize) {
            List<ItemDto> row = new ArrayList<>(items.subList(i, Math.min(i + rowSize, items.size())));

            // добавляем заглушки
            while (row.size() < rowSize) {
                row.add(ItemDto.stub()); // id = -1
            }

            rows.add(row);
        }

        return rows;
    }


}
