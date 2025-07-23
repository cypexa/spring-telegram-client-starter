package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class StickerResponseDto {
    private int fileId;
    private String fileUniqueId;
    private String emoji;
    private String setName;
    private boolean isAnimated;
    private boolean isVideo;
    private int width;
    private int height;
    private String thumbnailUrl;
    private String fileUrl;
} 