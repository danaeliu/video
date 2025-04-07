package com.demos.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.demos.video.demos.web.controller.demo",
        "com.demos.video.demos.web.controller.video","com.demos.video.demos.web.service",
        "com.demos.video.demos.web.controller.image"})
public class VideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
    }

}
