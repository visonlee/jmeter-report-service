package com.lws.jmeter.repository;

import com.lws.jmeter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}