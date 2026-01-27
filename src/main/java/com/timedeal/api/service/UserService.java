package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 사용자 회원가입
     * 
     * @param request: 회원가입 요청 (이메일, 비밀번호, 이름)
     * @return UserResponse (생성된 사용자 정보)
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        // 비밀번호 암호화
        // - passwordEncoder.encode(): 평문 비밀번호를 BCrypt로 암호화
        // - 암호화된 비밀번호는 DB에 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword) // 암호화된 비밀번호 저장
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
    
    /**
     * 전체 사용자 목록 조회 (관리자 전용, 페이징)
     * 
     * @param pageable: 페이징 정보 (page, size, sort)
     * @return Page<UserResponse> (모든 사용자 목록 + 페이징 정보)
     */
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::new);
    }
}
