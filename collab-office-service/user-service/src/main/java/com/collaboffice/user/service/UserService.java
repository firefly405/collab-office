package com.collaboffice.user.service;

import com.collaboffice.user.dto.*;
import com.collaboffice.user.vo.LoginResponse;
import com.collaboffice.user.vo.RegisterResponse;
import com.collaboffice.user.vo.UserInfoVO;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(Long userId, String accessToken);

    UserInfoVO getUserInfo(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    LoginResponse completeMfaLogin(Long userId);
}
