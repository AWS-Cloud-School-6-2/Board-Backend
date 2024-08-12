package com.mysite.sbb.user;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class LoginRequest {
    // Getter와 Setter 추가
    private String username;
    private String password;

}