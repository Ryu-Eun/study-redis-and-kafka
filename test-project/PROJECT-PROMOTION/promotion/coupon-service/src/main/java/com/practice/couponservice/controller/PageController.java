package com.practice.couponservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// templates의 coupon.html, policy.html 테스트를 위한 컨트롤러
@Controller
public class PageController {

    @GetMapping("/ui/coupons")
    public String couponPage() {
        return "coupon";
    }

    @GetMapping("/ui/policies")
    public String policyPage() {
        return "policy";
    }
}