package com.small.backend.orderservice.dao;

import com.small.backend.orderservice.entity.Order;
import com.small.backend.orderservice.entity.OrderPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends CassandraRepository<Order, OrderPrimaryKey> {
    List<Order> findByKeyUserEmail(String userEmail);
}
