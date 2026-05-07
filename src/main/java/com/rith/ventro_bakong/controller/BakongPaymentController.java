package com.rith.ventro_bakong.controller;


import com.rith.ventro_bakong.dto.BakongPaymentRequest;
import com.rith.ventro_bakong.dto.BakongPaymentResponse;
import com.rith.ventro_bakong.model.Order;
import com.rith.ventro_bakong.model.Payment;
import com.rith.ventro_bakong.service.BakongPaymentService;
import com.rith.ventro_bakong.service.OrderService;
import com.rith.ventro_bakong.service.PaymentService;
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
@RequestMapping("/api/payments/bakong")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Bakong Payment", description = "Bakong KHQR payment APIs")
public class BakongPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(BakongPaymentController.class);

    @Autowired
    private BakongPaymentService bakongPaymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/generate-qr")
    @Operation(summary = "Generate Bakong KHQR code for an order")
    public ResponseEntity<?> generateQRCode(@RequestBody BakongPaymentRequest request) {
        try {
            BakongPaymentResponse response = bakongPaymentService.generateQRCode(
                    request.getOrderId(),
                    request.getCurrency()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error generating QR for order: {}", request.getOrderId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error generating QR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate QR: " + e.getMessage()));
        }
    }

    @GetMapping("/config")
    @Operation(summary = "Get current Bakong payment configuration")
    public ResponseEntity<?> getConfig() {
        try {
            return ResponseEntity.ok(bakongPaymentService.getConfig());
        } catch (Exception e) {
            logger.error("Error getting config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve configuration: " + e.getMessage()));
        }
    }

    @PostMapping("/config")
    @Operation(summary = "Update Bakong payment configuration")
    public ResponseEntity<?> updateConfig(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bakong configuration parameters. Examples for terminalLabel: 'Cashier_1', 'Cashier_2', 'POS-001', 'POS-002', 'Counter_A', 'Counter_B', 'Terminal_1', 'Kiosk_1'",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\n" +
                                            "  \"bakongAccountId\": \"punoy@aclb\",\n" +
                                            "  \"accountPhone\": \"8551698764\",\n" +
                                            "  \"acquiringBank\": \"ACLEDA\",\n" +
                                            "  \"merchantName\": \"Ventro\",\n" +
                                            "  \"merchantCity\": \"Phnom Penh\",\n" +
                                            "  \"storeLabel\": \"Ventro\",\n" +
                                            "  \"terminalLabel\": \"Cashier_1\"\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody Map<String, String> request) {
        try {
            bakongPaymentService.updateConfig(
                    request.get("bakongAccountId"),
                    request.get("accountPhone"),
                    request.get("acquiringBank"),
                    request.get("merchantName"),
                    request.get("merchantCity"),
                    request.get("storeLabel"),
                    request.get("terminalLabel")
            );
            return ResponseEntity.ok(Map.of("message", "Configuration successfully updated"));
        } catch (Exception e) {
            logger.error("Error updating config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update configuration: " + e.getMessage()));
        }
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify payment status using MD5")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String md5,
            @RequestParam Long orderId) {
        try {
            boolean paid = bakongPaymentService.verifyPayment(md5, orderId);

            Order order = orderService.getOrderById(orderId.intValue());
            List<Payment> payments = paymentService.getPaymentsByOrderId(orderId.intValue());
            Payment bakongPayment = payments.stream()
                    .filter(p -> "BAKONG".equals(p.getPaymentMethod()))
                    .findFirst()
                    .orElse(null);

            String paymentStatus = bakongPayment != null ? bakongPayment.getPaymentStatus() : "NOT_FOUND";

            return ResponseEntity.ok(Map.of(
                    "orderId",       orderId,
                    "paid",          paid,
                    "paymentStatus", paymentStatus,
                    "orderStatus",   order != null ? order.getStatus() : "UNKNOWN"
            ));
        } catch (Exception e) {
            logger.error("Error verifying payment for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify payment: " + e.getMessage()));
        }
    }

    @PostMapping("/complete")
    @Operation(summary = "Manually mark a payment as completed")
    public ResponseEntity<?> completePayment(@RequestBody Map<String, Object> request) {
        try {
            Integer orderId = Integer.valueOf(request.get("orderId").toString());

            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }

            List<Payment> existing = paymentService.getPaymentsByOrderId(orderId);
            Payment payment = existing.stream()
                    .filter(p -> "BAKONG".equals(p.getPaymentMethod()))
                    .findFirst()
                    .orElse(null);

            if (payment == null) {
                payment = new Payment();
                payment.setOrderId(orderId);
                payment.setAmount(order.getTotalAmount());
                payment.setPaymentMethod("BAKONG");
                payment.setPaymentStatus("COMPLETED");
                paymentService.createPayment(payment);
            } else {
                payment.setPaymentStatus("COMPLETED");
                paymentService.updatePayment(payment.getId(), payment);
            }

            order.setStatus("PAID");
            orderService.updateOrder(orderId, order);

            return ResponseEntity.ok(Map.of(
                    "message",       "Payment completed",
                    "orderId",       orderId,
                    "paymentStatus", "COMPLETED",
                    "orderStatus",   "PAID"
            ));
        } catch (Exception e) {
            logger.error("Error completing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete payment: " + e.getMessage()));
        }
    }

    @PostMapping("/check-account")
    @Operation(summary = "Check Bakong Account", description = "Check if a bakong account exists")
    public ResponseEntity<?> checkAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bakong Account ID to check",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\n  \"accountId\": \"user@bank\"\n}"
                            )
                    )
            )
            @RequestBody Map<String, String> request) {
        try {
            String accountId = request.get("accountId");
            if (accountId == null || accountId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "accountId is required"));
            }

            boolean exists = bakongPaymentService.checkBakongAccount(accountId);

            if (exists) {
                // Use a HashMap to allow null values
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("responseCode", 0);
                response.put("responseMessage", "Account ID exists");
                response.put("errorCode", null);
                response.put("data", null);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("responseCode", 1);
                response.put("responseMessage", "Account ID not found");
                response.put("errorCode", 11);
                response.put("data", null);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error checking account", e);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("responseCode", 1);
            response.put("responseMessage", "Error checking account: " + e.getMessage());
            response.put("errorCode", 99);
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

