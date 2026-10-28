package com.example.Review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewEntityRepository reviewRepository;

    public ReviewEntity createReview(
            Long customerId,
            Long orderId,
            Integer rating,
            String comment
    ){
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }
        ReviewEntity review = ReviewEntity.builder()
                .customerId(customerId)
                .orderId(orderId)
                .rating(rating)
                .comment(comment)
                .build();
        return reviewRepository.save(review);
    }
}
