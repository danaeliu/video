package com.demos.video.demos.web.controller.demo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Arrays;

@Controller
public class HelloController {
    private final ResourceLoader resourceLoader;

    // 构造器注入ResourceLoader
    public HelloController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/hello") // 修正路径符号
    public String hello(Model model) {
        // 检查模板是否存在
        Resource resource = resourceLoader.getResource("classpath:/templates/hello.html");
        System.out.println("模板是否存在: " + resource.exists()); // 验证输出应为true

        // 修正属性添加方式
        model.addAttribute("message", "欢迎使用Thymeleaf！");
        model.addAttribute("userList", Arrays.asList("张三", "李四", "王五"));

        return "hello"; // 视图名称必须全小写，对应hello.html
    }
}