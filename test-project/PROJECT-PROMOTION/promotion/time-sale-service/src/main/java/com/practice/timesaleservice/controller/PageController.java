package com.practice.timesaleservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/ui/product")
    public String productPage(){
        return "product";
    }

    @GetMapping("/ui/time-sale")
    public String timeSale(){
        return "timeSale";
    }

    @GetMapping("/ui/async-time-sale")
    public String asyncTimeSale(){
        return "timesale";
    }


}
