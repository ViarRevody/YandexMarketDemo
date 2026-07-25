package com.example.api;

import com.example.domain.db.OrderEntityMapper;
import com.example.domain.OrderProcessor;
import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProcessor orderProcessor;
    private final OrderEntityMapper orderEntityMapper;

   @PostMapping
   public OrderDto create(@RequestBody CreateOrderRequestDto request){
       log.info("Creating order: request={}", request);
       var saved = orderProcessor.create(request);
       return orderEntityMapper.toOrderDto(saved);
   }


    @PostMapping("/{id}/pay")
    public OrderDto payOrder(
            @PathVariable Long id,
            @RequestBody OrderPaymentRequest request) {
        log.info("Paying order with id= {}, request={}",id, request);
        var entity=orderProcessor.processPayment(id,request);
        return orderEntityMapper.toOrderDto(entity);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(
            @PathVariable Long id) {
        log.info("Retrieving order with {}", id);
        var found = orderProcessor.getOrderOrThrow(id);
        return orderEntityMapper.toOrderDto(found);
    }
}
