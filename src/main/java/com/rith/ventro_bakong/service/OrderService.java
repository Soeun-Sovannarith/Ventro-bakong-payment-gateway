package com.rith.ventro_bakong.service;

import com.rith.ventro_bakong.mapper.OrderMapper;
import com.rith.ventro_bakong.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    public Order getOrderById(Integer id) {
        return orderMapper.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderMapper.findAll();
    }

    public Order createOrder(Order order) {
        order.setStatus("PENDING");
        order.setCreatedAt(new Date());
        orderMapper.insert(order);
        return order;
    }

    public Order updateOrder(Integer id, Order order) {
        order.setId(id);
        orderMapper.update(order);
        return order;
    }

    public void deleteOrder(Integer id) {
        orderMapper.delete(id);
    }
}
