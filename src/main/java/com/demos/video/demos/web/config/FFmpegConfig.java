package com.demos.video.demos.web.config;

import org.bytedeco.ffmpeg.global.avutil;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class FFmpegConfig {
    @PostConstruct
    public void initFFmpeg() {
        // 解决JavaCV的日志输出问题
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
    }
}