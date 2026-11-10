package com.collaboffice.user.controller;

import com.collaboffice.user.service.*;
import com.collaboffice.user.vo.RegisterResponse;
import com.collaboffice.common.util.JwtTokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private final UserService users=mock(UserService.class);
    private MockMvc mvc;
    @BeforeEach void setup(){ mvc=MockMvcBuilders.standaloneSetup(new AuthController(users,mock(TokenService.class),mock(JwtTokenUtil.class)))
        .setControllerAdvice(new com.collaboffice.common.handler.GlobalExceptionHandler()).build(); }

    @Test void localRegistrationDoesNotRequirePhoneOrSms() throws Exception {
        when(users.register(any())).thenReturn(RegisterResponse.builder().userId(1L).accessToken("test-token").build());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"localuser\",\"password\":\"Password123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.accessToken").value("test-token"));
        verify(users).register(any());
    }
    @Test void shortPasswordRejectedBeforeCallingService() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"localuser\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }
    @Test void externalLoginRoutesRemoved() throws Exception {
        mvc.perform(get("/api/v1/auth/oauth/wechat/initiate")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/auth/send-sms-code")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/auth/mfa/setup")).andExpect(status().isNotFound());
    }
}
