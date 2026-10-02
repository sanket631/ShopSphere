package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.RatingSummaryResponse;
import com.backend.ShopSphere.dto.ReviewRequest;
import com.backend.ShopSphere.dto.ReviewResponse;
import com.backend.ShopSphere.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> addReview(@Valid @RequestBody ReviewRequest request){
        reviewService.addReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request){
        reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId){
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        return ResponseEntity.ok(
                reviewService.getReviews(productId,page,size)
        );
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummary(
            @PathVariable UUID productId){

        return ResponseEntity.ok(
                reviewService.getRatingSummary(productId)
        );
    }
}