package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.model.Paging;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemsController.class)
class ItemsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private ItemService itemService;

    @Test
    void getItems_shouldReturnItemsPageWithItemsAndTotal() throws Exception {
        List<ItemDto> items = List.of(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0)
        );

        Page<ItemDto> page = new PageImpl<>(items);

        when(itemService.findItems("", ItemSort.NO, 1, 5))
                .thenReturn(page);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", ItemSort.NO))
                .andExpect(model().attributeExists("paging"));

        verify(itemService).findItems("", ItemSort.NO, 1, 5);
    }

    @Test
    void getItems_rootPath_shouldWorkSameAsItems() throws Exception {
        when(itemService.findItems(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", ItemSort.NO))
                .andExpect(model().attributeExists("paging"));
    }

    @Test
    void getItems_withSearchAndSort_shouldPassParamsToService() throws Exception {
        when(itemService.findItems("phone", ItemSort.PRICE, 2, 10))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/items")
                        .param("search", "phone")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("search", "phone"))
                .andExpect(model().attribute("sort", ItemSort.PRICE))
                .andExpect(model().attributeExists("paging"));

        verify(itemService).findItems("phone", ItemSort.PRICE, 2, 10);
    }

    @Test
    void getItems_shouldSplitItemsIntoRowsOfThree() throws Exception {
        List<ItemDto> items = List.of(
                new ItemDto(1L, "1", "", "", 100, 0),
                new ItemDto(2L, "2", "", "", 100, 0),
                new ItemDto(3L, "3", "", "", 100, 0),
                new ItemDto(4L, "4", "", "", 100, 0)
        );

        when(itemService.findItems(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(items));

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(2)))
                .andExpect(model().attribute("items", everyItem(hasSize(lessThanOrEqualTo(3)))));
    }


    @Test
    void getItems_shouldSetPagingCorrectly() throws Exception {
        Page<ItemDto> page = new PageImpl<>(
                List.of(),
                PageRequest.of(2, 5),
                20
        );

        when(itemService.findItems(any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attribute(
                        "paging",
                        samePropertyValuesAs(new Paging(5, 2, true, true))
                ));
    }

    @Test
    void getItemById_shouldReturnItemCorrectly() throws Exception {
        ItemDto item =  new ItemDto(1L, "1", "", "", 100, 0);

        when(itemService.getItemById(1L)).thenReturn(item);

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", sameInstance(item)));

        verify(itemService, times(1)).getItemById(1L);
        verifyNoMoreInteractions(itemService);
    }
}
