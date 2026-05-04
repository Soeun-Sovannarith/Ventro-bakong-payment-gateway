package com.rith.ventro_bakong.mapper;

import com.rith.ventro_bakong.model.Payment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PaymentMapper {

    @Select("SELECT * FROM payments WHERE id = #{id}")
    Payment findById(Integer id);

    @Select("SELECT * FROM payments WHERE order_id = #{orderId}")
    List<Payment> findByOrderId(Integer orderId);

    @Insert("INSERT INTO payments (order_id, amount, payment_method, payment_status, md5, qr_code, created_at, updated_at) " +
            "VALUES (#{orderId}, #{amount}, #{paymentMethod}, #{paymentStatus}, #{md5}, #{qrCode}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment payment);

    @Update("UPDATE payments SET payment_status = #{paymentStatus}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(Payment payment);

    @Delete("DELETE FROM payments WHERE id = #{id}")
    int delete(Integer id);
}
