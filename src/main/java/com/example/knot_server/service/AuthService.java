package com.example.knot_server.service;

import com.example.knot_server.controller.dto.RegisterResponse;
import com.example.knot_server.controller.dto.TokenResponse;

/**
 * AuthService interface for user authentication operations.
 */
public interface AuthService {

    /**
     * register a new user
     * 
     * @param username    the username of the new user
     * @param password    the password of the new user
     * @return           true if registration is successful, false otherwise
     */
    RegisterResponse register(String username, String password);

    /**
     * login a user
     * 
     * @param username    the username of the user
     * @param password    the password of the user
     * @return            the User object if login is successful, null otherwise
     */
    TokenResponse login(String username, String password);

    /**
     * logout a user
     * 
     * @param username    the username of the user
     * @return           true if logout is successful, false otherwise
     */
    boolean logout(String username);

    /**
     * refresh the access token
     * 
     * @param refreshToken the refresh token
     * @return            the new access token
     */
    TokenResponse refreshToken(String refreshToken);
}
