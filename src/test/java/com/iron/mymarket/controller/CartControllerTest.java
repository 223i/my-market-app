//package com.iron.mymarket.controller;
//
//import com.iron.mymarket.model.ItemAction;
//import com.iron.mymarket.model.ItemDto;
//import com.iron.mymarket.service.CartService;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.hamcrest.collection.IsEmptyCollection.empty;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(CartController.class)
//class CartControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private CartService cartService;
//
//    @Test
//    void getItemsInCart_shouldReturnCartPageWithItemsAndTotal() throws Exception {
//        // given
//        List<ItemDto> items = List.of(
//                new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 2),
//                new ItemDto(2L, "Item 2", "Desc", "img/2.jpg", 200L, 1)
//        );
//
//        Mockito.when(cartService.getCartItems()).thenReturn(items);
//        Mockito.when(cartService.getTotal()).thenReturn(400L);
//
//        // when / then
//        mockMvc.perform(get("/cart/items"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("items", items))
//                .andExpect(model().attribute("total", 400L));
//    }
//
//    @Test
//    void changeItemCountOnCartPage_shouldUpdateCartAndReturnCartPage() throws Exception {
//        // given
//        List<ItemDto> updatedItems = List.of(
//                new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 3)
//        );
//
//        Mockito.when(cartService.getCartItems()).thenReturn(updatedItems);
//        Mockito.when(cartService.getTotal()).thenReturn(300L);
//
//        // when / then
//        mockMvc.perform(post("/cart/items")
//                        .param("id", "1")
//                        .param("action", "PLUS"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("items", updatedItems))
//                .andExpect(model().attribute("total", 300L));
//
//        Mockito.verify(cartService).changeItemCount(1L, ItemAction.PLUS);
//    }
//
//    @Test
//    void changeItemCountOnCartPage_shouldDeleteItemFromCart() throws Exception {
//        // given
//        Mockito.when(cartService.getCartItems()).thenReturn(List.of());
//        Mockito.when(cartService.getTotal()).thenReturn(0L);
//
//        // when / then
//        mockMvc.perform(post("/cart/items")
//                        .param("id", "1")
//                        .param("action", "DELETE"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("items", empty()))
//                .andExpect(model().attribute("total", 0L));
//
//        Mockito.verify(cartService).changeItemCount(1L, ItemAction.DELETE);
//    }
//
//    @Test
//    void changeItemCountOnCartPage_shouldReturnBadRequestForInvalidAction() throws Exception {
//        mockMvc.perform(post("/cart/items")
//                        .param("id", "1")
//                        .param("action", "INVALID_ACTION"))
//                .andExpect(status().isBadRequest());
//
//        Mockito.verify(cartService, Mockito.never())
//                .changeItemCount(Mockito.anyLong(), Mockito.any());
//    }
//
//    @Test
//    void changeItemCountOnCartPage_shouldCallServiceMethodsInCorrectOrder() throws Exception {
//        // given
//        Mockito.when(cartService.getCartItems()).thenReturn(List.of());
//        Mockito.when(cartService.getTotal()).thenReturn(0L);
//
//        // when
//        mockMvc.perform(post("/cart/items")
//                        .param("id", "1")
//                        .param("action", "MINUS"))
//                .andExpect(status().isOk());
//
//        // then
//        var inOrder = Mockito.inOrder(cartService);
//
//        inOrder.verify(cartService).changeItemCount(1L, ItemAction.MINUS);
//        inOrder.verify(cartService).getCartItems();
//        inOrder.verify(cartService).getTotal();
//    }
//
//    @Test
//    void getItemsInCart_shouldReturnEmptyCart() throws Exception {
//        // given
//        Mockito.when(cartService.getCartItems()).thenReturn(List.of());
//        Mockito.when(cartService.getTotal()).thenReturn(0L);
//
//        // when / then
//        mockMvc.perform(get("/cart/items"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("items", empty()))
//                .andExpect(model().attribute("total", 0L));
//    }
//
//
//
//}
