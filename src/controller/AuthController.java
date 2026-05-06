package controller;

import dao.UserDAO;

public class AuthController {
    private final UserDAO userDAO;

    public AuthController() {
        this.userDAO = new UserDAO();
    }

    public boolean login(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        return userDAO.isValidUser(username.trim(), password);
    }
}
