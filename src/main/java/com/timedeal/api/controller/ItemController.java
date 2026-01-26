package com.timedeal.api.controller;

import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품(Item) 관련 REST API Controller
 * 
 * @RestController: 
 * - @Controller + @ResponseBody의 조합
 * - 메서드의 반환값을 HTTP Response Body에 자동으로 JSON/XML로 변환
 * - RESTful API를 만들 때 사용
 * 
 * @RequestMapping("/api/items"):
 * - 클래스 레벨에서 공통 URL 경로 설정
 * - 모든 메서드의 URL 앞에 "/api/items"가 붙음
 * 
 * @RequiredArgsConstructor:
 * - Lombok 어노테이션
 * - final 필드에 대한 생성자를 자동 생성
 * - 의존성 주입(DI)을 위한 생성자 주입 방식 사용
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    
    /**
     * ItemService 의존성 주입
     * 
     * final 키워드: 불변성 보장, 생성자 주입 방식 사용
     * Spring이 자동으로 ItemService 구현체를 주입해줌 (의존성 주입)
     */
    private final ItemService itemService;
    
    /**
     * 상품 등록 API
     * 
     * @PostMapping: HTTP POST 요청을 처리
     * - URL: POST /api/items
     * 
     * @Valid: 
     * - DTO의 유효성 검증 활성화
     * - ItemRequest의 @NotNull, @NotBlank 등의 검증 실행
     * - 검증 실패 시 400 Bad Request 반환
     * 
     * @RequestBody:
     * - HTTP 요청 본문(JSON)을 ItemRequest 객체로 자동 변환
     * - Content-Type: application/json 필요
     * 
     * ResponseEntity:
     * - HTTP 응답을 세밀하게 제어할 수 있는 클래스
     * - 상태 코드, 헤더, 본문을 모두 설정 가능
     * - HttpStatus.CREATED: 201 상태 코드 (리소스 생성 성공)
     */
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 상품 조회 API (단건)
     * 
     * @GetMapping("/{id}"): 
     * - HTTP GET 요청 처리
     * - URL: GET /api/items/{id}
     * - {id}는 경로 변수(Path Variable)
     * 
     * @PathVariable:
     * - URL 경로의 {id} 값을 메서드 파라미터로 받음
     * - 예: /api/items/1 → id = 1
     * 
     * ResponseEntity.ok():
     * - 200 OK 상태 코드와 함께 응답 반환
     * - 간단한 성공 응답에 사용
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id) {
        ItemResponse response = itemService.getItem(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 전체 상품 목록 조회 API
     * 
     * @GetMapping:
     * - HTTP GET 요청 처리
     * - URL: GET /api/items
     * - 경로 변수가 없으므로 클래스 레벨 경로만 사용
     */
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> responses = itemService.getAllItems();
        return ResponseEntity.ok(responses);
    }
}
