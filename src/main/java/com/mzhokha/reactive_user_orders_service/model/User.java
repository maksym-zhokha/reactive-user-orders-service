package com.mzhokha.reactive_user_orders_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document("users")
public record User(@Id String id, String name, String phone) { }
