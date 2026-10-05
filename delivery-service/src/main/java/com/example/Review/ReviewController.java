package com.example.Review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ReviewEntity createReview(
            @RequestParam Long customerId,
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment
    ) {

        return reviewService.createReview(
                customerId,
                orderId,
                rating,
                comment
        );
    }
}
