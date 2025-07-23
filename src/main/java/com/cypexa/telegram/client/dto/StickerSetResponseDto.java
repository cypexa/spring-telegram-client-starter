package com.cypexa.telegram.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class StickerSetResponseDto {
    private long id;
    private String name;
    private String title;
    private boolean isInstalled;
    private boolean isOfficial;
    private boolean isAnimated;
    private boolean isVideo;
    private List<StickerResponseDto> stickers;
    private int stickerCount;
    
    public static StickerSetResponseDto success(long id, String name, String title) {
        StickerSetResponseDto response = new StickerSetResponseDto();
        response.setId(id);
        response.setName(name);
        response.setTitle(title);
        return response;
    }
} 