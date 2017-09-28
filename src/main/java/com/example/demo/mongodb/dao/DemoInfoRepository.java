package com.example.demo.mongodb.dao;

import com.example.demo.mongodb.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface  DemoInfoRepository  extends MongoRepository<UserEntity, String> {
    public UserEntity findByUserName(String name);
}