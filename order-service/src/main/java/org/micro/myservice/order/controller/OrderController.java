package org.micro.myservice.order.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.order.dto.ApiResponse;
import org.micro.myservice.order.dto.CreateOrderRequest;
import org.micro.myservice.order.dto.OrderResponse;
import org.micro.myservice.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id()))
                .body(ApiResponse.success(order));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> find(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.find(orderId));
    }
}
