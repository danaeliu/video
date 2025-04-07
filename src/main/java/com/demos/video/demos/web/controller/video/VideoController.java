package com.demos.video.demos.web.controller.video;

import com.demos.video.demos.web.service.video.VideoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/video")
@Validated
public class VideoController {

    private final VideoService videoService;


    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public String videoPage() {
        return "video";
    }

    @PostMapping("/generate")
    public String generateVideo(
            @RequestParam @NotBlank String textContent,
            RedirectAttributes redirectAttributes) throws Exception {
        String fileName = videoService.generateVideo(textContent);
//        System.out.println("视频文件名是: " + fileName + "\n");
        redirectAttributes.addFlashAttribute("videoFileName", fileName);
        return "redirect:/video";
    }

    @GetMapping("/preview")
    public ResponseEntity<Resource> previewVideo(@RequestParam String videoFileName) {
// 使用@RequestParam
        Path path = Paths.get(videoService.getOutputPath(), videoFileName);
        System.out.println("controllerpreviewVideo视频文件路径是: " + path.toString());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadVideo(@RequestParam String videoFileName) {
        try {
            Path path = Paths.get(videoService.getOutputPath(), videoFileName);

            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(videoFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"" + encodedFileName + "\"")
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .body(new FileSystemResource(path));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}