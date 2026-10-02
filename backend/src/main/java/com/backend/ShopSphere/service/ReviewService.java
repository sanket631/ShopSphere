package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.RatingSummaryResponse;
import com.backend.ShopSphere.dto.ReviewImageRequest;
import com.backend.ShopSphere.dto.ReviewRequest;
import com.backend.ShopSphere.dto.ReviewResponse;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.events.ReviewCreatedEvent;
import com.backend.ShopSphere.events.ReviewDeletedEvent;
import com.backend.ShopSphere.events.ReviewUpdatedEvent;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = {
            "product",
            "product-search",
            "product-details"
    }, allEntries = true)
    public void addReview(ReviewRequest request) {
        User user = getCurrentUser();

        log.info("Creating review. user={}, orderItemId={}",
                user.getEmail(),
                request.getOrderItemId());

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId()).orElseThrow(() -> {
            log.error("Order item not found. orderItemId={}", request.getOrderItemId());
            return new ResourceNotFoundException("Order item not found");
        });

        validateReviewRequest(user, orderItem);

        ProductVariant variant = productVariantRepository.findById(orderItem.getProductVariantId()).orElseThrow(
                () -> new ResourceNotFoundException("Product Variant not found")
        );

        Product product = variant.getProduct();

        if (reviewRepository.existsByUserAndProduct(user, product)) {
            log.warn("Duplicate review attempt. user={}, productId={}", user.getEmail(), product.getId());
            throw new ConflictException("You have already reviewed this product.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .purchasedVariant(variant)
                .rating(request.getRating())
                .description(request.getDescription())
                .build();

        review.setReviewImages(
                buildReviewImages(
                        request.getReviewImageRequests(),
                        review
                )
        );

        Review saved = reviewRepository.save(review);

        BigDecimal newAverageRatingForProduct = recalculateProductRating(request.getRating(), 0, product.getRatingSum(), product.getReviewCount() + 1);
        product.setReviewCount(product.getReviewCount() + 1);
        product.setAverageRating(newAverageRatingForProduct);
        product.setRatingSum(product.getRatingSum() + request.getRating());
        productRepository.save(product);

        eventPublisher.publishEvent(
                new ReviewCreatedEvent(
                        UUID.randomUUID(),
                        saved.getId(),
                        product.getId(),
                        user.getId()
                )
        );

        log.info(
                "Review created successfully. reviewId={}, productId={}, rating={}",
                saved.getId(),
                product.getId(),
                saved.getRating());

    }

    @Transactional
    @CacheEvict(value = {
            "product",
            "product-search",
            "product-details"
    }, allEntries = true)
    public void updateReview(UUID reviewId, ReviewRequest request) {
        User user = getCurrentUser();
        Review review = reviewRepository.findByIdAndUser(reviewId, user).orElseThrow(() -> {
                            log.error("Review not found. reviewId={}", reviewId);
                            return new ResourceNotFoundException("Review not found");
                        });

        log.info("Updating review. reviewId={}, user={}", reviewId, user.getEmail());

        review.setDescription(request.getDescription());

        List<ReviewImage> images = buildReviewImages(request.getReviewImageRequests(), review);
        review.getReviewImages().clear();
        review.getReviewImages().addAll(images);

        reviewRepository.save(review);

        if(!request.getRating().equals(review.getRating())){
            review.setRating(request.getRating());
            Product product = review.getProduct();
            BigDecimal newAverageRatingForProduct = recalculateProductRating(request.getRating(), review.getRating(), product.getRatingSum(), product.getReviewCount());
            product.setAverageRating(newAverageRatingForProduct);
            product.setRatingSum(product.getRatingSum() - review.getRating() + request.getRating());
            productRepository.save(product);
        }


        eventPublisher.publishEvent(new ReviewUpdatedEvent(UUID.randomUUID(), review.getId(), review.getProduct().getId(), user.getId()));

        log.info("Review updated successfully. reviewId={}", reviewId);

    }

    @Transactional
    @CacheEvict(value = {
            "product",
            "product-search",
            "product-details"
    }, allEntries = true)
    public void deleteReview(UUID reviewId) {
        User user = getCurrentUser();

        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Product product = review.getProduct();
        log.info("Deleting review. reviewId={}, productId={}", reviewId, product.getId());


        BigDecimal newAverageRatingForProduct = recalculateProductRating(0, review.getRating(), product.getRatingSum(), product.getReviewCount() - 1);
        product.setReviewCount(product.getReviewCount() - 1);
        product.setAverageRating(newAverageRatingForProduct);
        product.setRatingSum(product.getRatingSum() - review.getRating());
        productRepository.save(product);

        reviewRepository.delete(review);

        eventPublisher.publishEvent(
                new ReviewDeletedEvent(
                        UUID.randomUUID(),
                        reviewId,
                        product.getId(),
                        user.getId()
                )
        );

        log.info("Review deleted successfully. reviewId={}", reviewId);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(UUID productId,int page,int size){

        Product product=productRepository.findById(productId).orElseThrow(
                ()->new ResourceNotFoundException("Product not found"));

        Pageable pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"));

        return reviewRepository.findByProduct(product,pageable)
                .map(this::mapToReviewResponse);
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<Object[]> ratingCounts = reviewRepository.countByRatingGrouped(product);

        Map<Integer, Long> ratingMap = ratingCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (Integer) arr[0],
                        arr -> (Long) arr[1]
                ));

        return RatingSummaryResponse.builder()
                .averageRating(product.getAverageRating())
                .totalReviews(product.getReviewCount().longValue())
                .fiveStar(ratingMap.getOrDefault(5, 0L))
                .fourStar(ratingMap.getOrDefault(4, 0L))
                .threeStar(ratingMap.getOrDefault(3, 0L))
                .twoStar(ratingMap.getOrDefault(2, 0L))
                .oneStar(ratingMap.getOrDefault(1, 0L))
                .build();
    }

    private BigDecimal recalculateProductRating(int newRating, int oldRating, int ratingSum, int ratingCount){
        if(ratingCount == 0){
            return BigDecimal.valueOf(0);
        }

        int newRatingSum = ratingSum - oldRating + newRating;
        return BigDecimal.valueOf(newRatingSum).divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }

    private List<ReviewImage> buildReviewImages(List<ReviewImageRequest> requests,Review review){

        if(requests==null||requests.isEmpty()){
            return new ArrayList<>();
        }

        return requests.stream()
                .map(request->ReviewImage.builder()
                        .review(review)
                        .fileName(request.getFileName())
                        .fileType(request.getFileType())
                        .fileSize(request.getFileSize())
                        .imageUrl(request.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateReviewRequest(User user,OrderItem orderItem){

        Order order=orderItem.getOrder();

        if(!order.getUser().getId().equals(user.getId())){
            throw new IllegalArgsException("You are not authorized to review this order");
        }

        if(!FulfillmentStatus.DELIVERED.equals(orderItem.getStatus())){
            throw new IllegalArgsException("Product is not delivered yet");
        }
    }

    private User getCurrentUser(){

        String email=securityUtil.getCurrentUserEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(
                        ()->new ResourceNotFoundException("User not found")
                );
    }

    private ReviewResponse mapToReviewResponse(Review review){

        List<String> images=review.getReviewImages()
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return ReviewResponse.builder()
                .reviewId(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .description(review.getDescription())
                .reviewImages(images)
                .reviewDate(review.getCreatedAt())
                .verifiedPurchase(true)
                .purchasedVariantSku(review.getPurchasedVariant().getSku())
                .variantAttributes(review.getPurchasedVariant().getAttributes())
                .build();
    }
}
