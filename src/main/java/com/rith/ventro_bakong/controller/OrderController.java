package com.rith.ventro_bakong.controller;

import com.rith.ventro_bakong.model.Order;
import com.rith.ventro_bakong.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error fetching orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<?> getOrderById(@PathVariable Integer id) {
        try {
            Order order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.error("Error fetching order: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order: " + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        try {
            Order created = orderService.createOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an order")
    public ResponseEntity<?> updateOrder(@PathVariable Integer id, @RequestBody Order order) {
        try {
            Order existing = orderService.getOrderById(id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }
            return ResponseEntity.ok(orderService.updateOrder(id, order));
        } catch (Exception e) {
            logger.error("Error updating order: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an order")
    public ResponseEntity<?> deleteOrder(@PathVariable Integer id) {
        try {
            Order existing = orderService.getOrderById(id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }
            orderService.deleteOrder(id);
            return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting order: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete order: " + e.getMessage()));
        }
    }
}
