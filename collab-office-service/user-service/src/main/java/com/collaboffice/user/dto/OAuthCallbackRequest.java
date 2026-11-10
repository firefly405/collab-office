package com.collaboffice.user.dto;

import lombok.Data;

@Data
public class OAuthCallbackRequest {
    private String code;
    private String state;
}
