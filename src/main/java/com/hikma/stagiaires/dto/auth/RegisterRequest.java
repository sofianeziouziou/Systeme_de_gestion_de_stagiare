package com.hikma.stagiaires.dto.auth;
import com.hikma.stagiaires.model.user.Role;
import lombok.Data;
@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;  // nécessaire pour SMS OTP
    private Role   role;
    private String departement;

}