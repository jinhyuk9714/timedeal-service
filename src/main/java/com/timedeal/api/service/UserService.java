package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // 실제로는 암호화 필요
                .name(request.getName())
                .build();
        
        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }
    
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserResponse(user);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
