package com.rith.ventro_bakong.service;

import com.rith.ventro_bakong.mapper.PaymentMapper;
import com.rith.ventro_bakong.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    public Payment getPaymentById(Integer id) {
        return paymentMapper.findById(id);
    }

    public List<Payment> getPaymentsByOrderId(Integer orderId) {
        return paymentMapper.findByOrderId(orderId);
    }

    public Payment createPayment(Payment payment) {
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());
        paymentMapper.insert(payment);
        return payment;
    }

    public Payment updatePayment(Integer id, Payment payment) {
        payment.setId(id);
        payment.setUpdatedAt(new Date());
        paymentMapper.update(payment);
        return payment;
    }
}
