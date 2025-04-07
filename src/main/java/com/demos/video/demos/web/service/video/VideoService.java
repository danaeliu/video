package com.demos.video.demos.web.service.video;

import lombok.RequiredArgsConstructor;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

// 保持原有类代码不变

@Service
@RequiredArgsConstructor
public class VideoService {
    @Value("${video.storage.path}")
    private String outputPath;

    @PostConstruct
    public void init() {
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("创建视频存储目录: " + dir.getAbsolutePath());

        }
        System.out.println("视频存储目录: " + dir.getAbsolutePath());

    }

    public String generateVideo(String text) throws Exception {
        String fileName = UUID.randomUUID() + ".mp4";
        File outputFile = new File(outputPath, fileName);
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                outputFile.getAbsolutePath(), 1280, 720
        );

        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(30);
        recorder.start();

        for (int i = 0; i < 300; i++) {
            Frame frame = renderTextFrame(text);
            recorder.record(frame);
        }

        recorder.stop();
        System.out.println("视频生成完成: " + outputFile.getAbsolutePath());
        System.out.println("视频文件名是: " + fileName);
        return fileName; // 返回完整路径
    }

    private Frame renderTextFrame(String text) {
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = image.createGraphics();
        
        // 文字渲染配置
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = new Font("微软雅黑", Font.BOLD, 40);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        
        // 文字布局
        FontMetrics metrics = g2d.getFontMetrics();
        int y = 100;
        for (String line : text.split("\n")) {
            g2d.drawString(line, 100, y);
            y += metrics.getHeight();
        }
        
        return new Java2DFrameConverter().convert(image);
    }

    public String getOutputPath() {
        return outputPath;
    }
}