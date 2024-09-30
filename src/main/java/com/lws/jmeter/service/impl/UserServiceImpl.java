package com.lws.jmeter.service.impl;

import com.lws.jmeter.entity.User;
import com.lws.jmeter.repository.UserRepository;
import com.lws.jmeter.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;

    @Override
    public User getUserById(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        return optionalUser.get();
    }
}
