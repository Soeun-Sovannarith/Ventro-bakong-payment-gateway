package com.rith.ventro_bakong.service;



import com.rith.ventro_bakong.dto.BakongPaymentResponse;
import com.rith.ventro_bakong.model.Order;
import com.rith.ventro_bakong.model.Payment;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class BakongPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(BakongPaymentService.class);

    @Value("${bakong.api.base.url}")
    private String bakongApiBaseUrl;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BakongTokenService bakongTokenService;

    @Value("${bakong.account.id}")
    private String bakongAccountId;

    @Value("${bakong.account.phone}")
    private String accountPhone;

    @Value("${bakong.acquiring.bank}")
    private String acquiringBank;

    @Value("${bakong.merchant.name}")
    private String merchantName;

    @Value("${bakong.merchant.city}")
    private String merchantCity;

    @Value("${bakong.store.label}")
    private String storeLabel;

    @Value("${bakong.terminal.label}")
    private String terminalLabel;

    public void updateConfig(String accountId, String phone, String bank, String merchant, String city, String store, String terminal) {
        if (accountId != null) this.bakongAccountId = accountId;
        if (phone != null) this.accountPhone = phone;
        if (bank != null) this.acquiringBank = bank;
        if (merchant != null) this.merchantName = merchant;
        if (city != null) this.merchantCity = city;
        if (store != null) this.storeLabel = store;
        if (terminal != null) this.terminalLabel = terminal;
    }

    public Map<String, String> getConfig() {
        return Map.of(
            "bakongAccountId", this.bakongAccountId != null ? this.bakongAccountId : "",
            "accountPhone", this.accountPhone != null ? this.accountPhone : "",
            "acquiringBank", this.acquiringBank != null ? this.acquiringBank : "",
            "merchantName", this.merchantName != null ? this.merchantName : "",
            "merchantCity", this.merchantCity != null ? this.merchantCity : "",
            "storeLabel", this.storeLabel != null ? this.storeLabel : "",
            "terminalLabel", this.terminalLabel != null ? this.terminalLabel : ""
        );
    }

    public BakongPaymentResponse generateQRCode(Long orderId, String currency) {
        Order order = orderService.getOrderById(orderId.intValue());
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        String finalCurrency = (currency == null) ? "USD" : currency;
        String billNumber    = "ORDER-" + orderId + "-" + System.currentTimeMillis();

        IndividualInfo individualInfo = new IndividualInfo();
        individualInfo.setBakongAccountId(bakongAccountId);
        individualInfo.setAccountInformation(accountPhone);
        individualInfo.setAcquiringBank(acquiringBank);
        individualInfo.setCurrency("USD".equals(finalCurrency) ? KHQRCurrency.USD : KHQRCurrency.KHR);
        individualInfo.setAmount(Double.valueOf(order.getTotalAmount()));

        // Add expiration timestamp (15 minutes from now)
        individualInfo.setExpirationTimestamp(System.currentTimeMillis() + 15 * 60 * 1000);

        individualInfo.setMerchantName(merchantName);
        individualInfo.setMerchantCity(merchantCity);
        individualInfo.setBillNumber(billNumber);
        individualInfo.setMobileNumber(accountPhone);
        individualInfo.setStoreLabel(storeLabel);
        individualInfo.setTerminalLabel(terminalLabel);

        KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);

        if (response == null) {
            throw new RuntimeException("KHQR generation returned null response");
        }
        if (response.getKHQRStatus().getCode() != 0) {
            throw new RuntimeException("KHQR generation failed: " + response.getKHQRStatus().getMessage());
        }

        KHQRData data = response.getData();
        logger.info("QR generated - md5: {}", data.getMd5());

        List<Payment> existing = paymentService.getPaymentsByOrderId(order.getId());
        boolean hasPending = existing.stream()
                .anyMatch(p -> "BAKONG".equals(p.getPaymentMethod())
                        && "PENDING".equals(p.getPaymentStatus()));

        if (!hasPending) {
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod("BAKONG");
            payment.setPaymentStatus("PENDING");
            payment.setMd5(data.getMd5());
            payment.setQrCode(data.getQr());
            paymentService.createPayment(payment);
            logger.info("Created PENDING payment for order: {}", orderId);
        }

        BakongPaymentResponse result = new BakongPaymentResponse();
        result.setQrCode(data.getQr());
        result.setMd5(data.getMd5());
        result.setBillNumber(billNumber);
        result.setAmount(Double.valueOf(order.getTotalAmount()));
        result.setCurrency(finalCurrency);
        return result;
    }

    public boolean verifyPayment(String md5, Long orderId) {
        try {
            String apiToken = bakongTokenService.getToken();

            String url = bakongApiBaseUrl.replaceAll("/+$", "") + "/v1/check_transaction_by_md5";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);

            logger.info("Calling Bakong API: {}", url);
            logger.info("Using token: {}", apiToken != null && apiToken.length() > 10 ? apiToken.substring(0, 10) + "..." : "NULL");

            Map<String, String> body = Map.of("md5", md5);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> apiResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (apiResponse.getStatusCode() != HttpStatus.OK || apiResponse.getBody() == null) {
                logger.warn("Bakong API returned non-200 or empty body: {}", apiResponse.getStatusCode());
                return false;
            }

            Map<String, Object> responseBody = apiResponse.getBody();
            logger.info("Bakong verify response: {}", responseBody);

            Object responseCode = responseBody.get("responseCode");
            String responseMessage = (String) responseBody.get("responseMessage");
            logger.info("responseCode: {}, responseMessage: {}", responseCode, responseMessage);

            boolean paid = Integer.valueOf(0).equals(responseCode);

            if (!paid) {
                logger.warn("Payment not successful: {}", responseMessage);
                return false;
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data == null) {
                logger.warn("No data in response");
                return false;
            }

            logger.info("Transaction - from: {}, to: {}, amount: {}, currency: {}",
                    data.get("fromAccountId"),
                    data.get("toAccountId"),
                    data.get("amount"),
                    data.get("currency"));

            List<Payment> payments = paymentService.getPaymentsByOrderId(orderId.intValue());
            payments.stream()
                    .filter(p -> "BAKONG".equals(p.getPaymentMethod()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setPaymentStatus("COMPLETED");
                        p.setMd5(md5); // Keep the latest MD5
                        paymentService.updatePayment(p.getId(), p);

                        Order order = orderService.getOrderById(orderId.intValue());
                        if (order != null) {
                            order.setStatus("PAID");
                            orderService.updateOrder(orderId.intValue(), order);
                        }
                    });

            return true;

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            logger.error("401 Unauthorized - token is invalid or expired. Renew at https://api-bakong.nbc.org.kh/");
            return false;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            logger.warn("Transaction not found for md5: {}", md5);
            return false;
        } catch (Exception e) {
            logger.error("Error calling Bakong verify API", e);
            return false;
        }
    }
}