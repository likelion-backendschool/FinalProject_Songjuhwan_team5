package com.ll.finalProject.week2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.finalProject.week2.domain.CartItem;
import com.ll.finalProject.week2.domain.Member;
import com.ll.finalProject.week2.domain.OrderItem;
import com.ll.finalProject.week2.domain.Ordered;
import com.ll.finalProject.week2.exception.OrderIdNotMatchedException;
import com.ll.finalProject.week2.exception.OrderNotEnoughRestCashException;
import com.ll.finalProject.week2.service.CartItemService;
import com.ll.finalProject.week2.service.MemberService;
import com.ll.finalProject.week2.service.OrderItemService;
import com.ll.finalProject.week2.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final MemberService memberService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createOrder(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        Member member = memberService.findByUserName(user.getUsername());
        orderService.createFromCart(member);
        return "redirect:/order/list";
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public String orderList(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        Member member = memberService.findByUserName(user.getUsername());
        List<Ordered> orderedList = orderService.findAllByMember(member);
        model.addAttribute("orderList", orderedList);
        return "order/list";
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public String orderDetail(@PathVariable Long orderId, Model model){
        Ordered ordered = orderService.findById(orderId);
        List<OrderItem> orderItemList = orderItemService.findAllByOrder(ordered);
        model.addAttribute("order", ordered);
        model.addAttribute("orderItemList", orderItemList);
        return "order/detail";
    }

    @PostConstruct
    private void init() {
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
            }
        });
    }

    private final String SECRET_KEY = "test_sk_D4yKeq5bgrp1N6OD5pQ8GX0lzW6Y";

    @RequestMapping("/{id}/success")
    public String confirmPayment(
            @PathVariable long id,
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam int amount,
            Model model
    ) throws Exception {

        Ordered order = orderService.findById(id);

        long orderIdInputed = Long.parseLong(orderId.split("__")[1]);

        if ( id != orderIdInputed ) {
            throw new OrderIdNotMatchedException();
        }


        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth(SECRET_KEY, ""); // spring framework 5.2 이상 버전에서 지원
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("orderId", orderId);
        payloadMap.put("amount", String.valueOf(amount));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        Member member = memberService.findByUserName(user.getUsername());

        int restCash = member.getRestCash(); // 사용자가 보유하고 있는 예치금
        int payPriceRestCash = order.getCalculatePayPrice() - amount; // 사용한 예치금의 금액

        if(payPriceRestCash > restCash) {// 사용한 예치금의 금액이 보유하고 있는 예치금보다 큰 경우
            throw new OrderNotEnoughRestCashException();
        }
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

        ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/" + paymentKey, request, JsonNode.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            orderService.waitToRefund(order);
            orderService.payByTossPayments(order, payPriceRestCash);
            return "order/success";
        } else {
            JsonNode failNode = responseEntity.getBody();
            model.addAttribute("message", failNode.get("message").asText());
            model.addAttribute("code", failNode.get("code").asText());
            return "order/fail";
        }
    }

    @RequestMapping("/{id}/fail")
    public String failPayment(@RequestParam String message, @RequestParam String code, Model model) {
        model.addAttribute("message", message);
        model.addAttribute("code", code);
        return "order/fail";
    }

    @PostMapping("/{orderId}/payByRestCashOnly")
    @PreAuthorize("isAuthenticated()")
    public String payByRestCashOnly(@PathVariable Long orderId){
        orderService.payByRestCashOnly(orderId);
        return "redirect:/order/%d".formatted(orderId);

    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancelOrder(@PathVariable Long orderId){
        orderService.cancel(orderId);
        return "redirect:/order/list";
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("isAuthenticated()")
    public String refundOrder(@PathVariable Long orderId){
        orderService.refund(orderId);
        return "redirect:/order/list";
    }

}
