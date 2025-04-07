package com.demos.video.demos.web.controller.image;

import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Controller
@Validated
public class ImageController {

    // 缓存配置
    private static final Map<String, ImageData> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final int MIN_FONT_SIZE = 12;
    private static final int MAX_FONT_SIZE = 72;

    @GetMapping("/image")
    public String imagePage() {
        return "image";
    }
    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<?> generateImage(
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam String text,
            @RequestParam String textColor,
            @RequestParam String bgColor,
            @RequestParam String format) {

        try {
            validateParams(width, height, text);
            BufferedImage image = createAdaptiveTextImage(width, height, text, textColor, bgColor);
            String imageId = cacheImage(image, format);

            return ResponseEntity.ok(Map.of(
                    "previewUrl", "/image/" + imageId,
                    "downloadUrl", "/download/" + imageId,
                    "expireTime", LocalDateTime.now().plusHours(1)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 核心图片生成方法（带自适应字体和智能换行）
    private BufferedImage createAdaptiveTextImage(int width, int height, String text,
                                                  String textColorHex, String bgColorHex) {
        Color textColor = parseHexColor(textColorHex);
        Color bgColor = parseHexColor(bgColorHex);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);

        // 动态计算最佳字体大小
        Font font = findOptimalFont(g, width, height, text);
        g.setFont(font);

        // 智能换行处理
        List<String> lines = wrapText(text, width - 40, g);
        FontMetrics fm = g.getFontMetrics();

        // 计算绘制位置（垂直居中）
        int totalHeight = fm.getHeight() * lines.size();
        int y = (height - totalHeight) / 2 + fm.getAscent();

        // 绘制多行文本
        g.setColor(textColor);
        for (String line : lines) {
            int x = (width - fm.stringWidth(line)) / 2;  // 水平居中
            g.drawString(line, x, y);
            y += fm.getHeight();
        }

        g.dispose();
        return image;
    }

    // 智能字体匹配算法
    private Font findOptimalFont(Graphics2D g, int width, int height, String text) {
        Font bestFont = null;
        int bestSize = MIN_FONT_SIZE;

        // 尝试系统可用字体
        String[] fontNames = {
                "微软雅黑", "宋体"
                , Font.SANS_SERIF
        };

        // 动态调整字体大小
        for (int size = MAX_FONT_SIZE; size >= MIN_FONT_SIZE; size -= 2) {
            for (String fontName : fontNames) {
                Font testFont = new Font(fontName, Font.PLAIN, size);
                g.setFont(testFont);

                // 检查是否满足尺寸要求
                if (checkTextFits(text, width - 40, height, g)) {
                    bestFont = testFont;
                    bestSize = size;
                    break;
                }
            }
            if (bestFont != null) break;
        }

        return bestFont != null ? bestFont : new Font(Font.SANS_SERIF, Font.PLAIN, MIN_FONT_SIZE);
    }

    // 智能换行算法（支持中英文混合）
    private List<String> wrapText(String text, int maxWidth, Graphics2D g) {
        List<String> lines = new ArrayList<>();
        FontMetrics fm = g.getFontMetrics();

        // 按换行符分割段落
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();

            // 按单词处理（支持中英文空格）
            for (String word : paragraph.split("(?<=\\S)(?=\\s)|(?<=\\s)(?=\\S)")) {
                if (fm.stringWidth(line + word) > maxWidth) {
                    if (line.length() > 0) {
                        lines.add(line.toString().trim());
                        line = new StringBuilder();
                    }
                    // 处理超长单词
                    while (fm.stringWidth(word) > maxWidth) {
                        int splitIndex = findSplitIndex(word, maxWidth, fm);
                        lines.add(word.substring(0, splitIndex));
                        word = word.substring(splitIndex);
                    }
                }
                line.append(word);
            }
            if (line.length() > 0) {
                lines.add(line.toString().trim());
            }
        }
        return lines;
    }

    // 辅助方法：检查文本是否适应画布
    private boolean checkTextFits(String text, int maxWidth, int maxHeight, Graphics2D g) {
        List<String> lines = wrapText(text, maxWidth, g);
        FontMetrics fm = g.getFontMetrics();
        return (fm.getHeight() * lines.size()) <= maxHeight;
    }

    // 辅助方法：查找单词拆分位置
    private int findSplitIndex(String word, int maxWidth, FontMetrics fm) {
        int low = 0;
        int high = word.length();
        while (low < high) {
            int mid = (low + high) / 2;
            if (fm.stringWidth(word.substring(0, mid)) < maxWidth) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low - 1;
    }

    // 以下方法保持原有实现不变（需完整保留）
    private void validateParams(int width, int height, String text) {
        if (width < 50 || width > 5000) throw new IllegalArgumentException("宽度需在50-5000像素之间");
        if (height < 50 || height > 5000) throw new IllegalArgumentException("高度需在50-5000像素之间");
        if (text.getBytes().length > MAX_TEXT_LENGTH) throw new IllegalArgumentException("文字长度超过限制");
    }

    private String cacheImage(BufferedImage image, String format) {
        String imageId = UUID.randomUUID().toString();
        if (IMAGE_CACHE.size() >= MAX_CACHE_SIZE) IMAGE_CACHE.clear();
        IMAGE_CACHE.put(imageId, new ImageData(imageToBytes(image, format), format, LocalDateTime.now().plusHours(1)));
        return imageId;
    }

    private byte[] imageToBytes(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("图片编码失败", e);
        }
    }

    private Color parseHexColor(String hex) {
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的颜色值: " + hex);
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        return handleImageRequest(id, false);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String id) {
        return handleImageRequest(id, true);
    }

    private ResponseEntity<byte[]> handleImageRequest(String id, boolean download) {
        ImageData data = IMAGE_CACHE.get(id);
        if (data == null || data.expiryTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/" + data.format));
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

        if (download) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("image." + data.format)
                    .build());
        }

        return new ResponseEntity<>(data.bytes, headers, HttpStatus.OK);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanImageCache() {
        IMAGE_CACHE.entrySet().removeIf(entry ->
                entry.getValue().expiryTime.isBefore(LocalDateTime.now())
        );
    }

    private static class ImageData {
        byte[] bytes;
        String format;
        LocalDateTime expiryTime;

        ImageData(byte[] bytes, String format, LocalDateTime expiryTime) {
            this.bytes = bytes;
            this.format = format;
            this.expiryTime = expiryTime;
        }
    }
}