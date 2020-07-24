package com.example.callee;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Class Controller
 */
@RestController
public class Controller
{

    @GetMapping("/api")
    public String testApi()
    {
        return "success";
    }
}
