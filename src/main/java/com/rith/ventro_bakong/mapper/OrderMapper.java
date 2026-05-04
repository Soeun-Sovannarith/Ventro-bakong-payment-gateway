package com.rith.ventro_bakong.mapper;


import com.rith.ventro_bakong.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(Integer id);

    @Select("SELECT * FROM orders ORDER BY created_at DESC")
    List<Order> findAll();

    @Insert("INSERT INTO orders (total_amount, status, created_at) " +
            "VALUES (#{totalAmount}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET total_amount = #{totalAmount}, status = #{status} " +
            "WHERE id = #{id}")
    int update(Order order);

    @Delete("DELETE FROM orders WHERE id = #{id}")
    int delete(Integer id);
}
