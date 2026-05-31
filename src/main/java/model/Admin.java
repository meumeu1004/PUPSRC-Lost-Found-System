package model;

import java.sql.Timestamp;

public class Admin {

    private int adminId;
    private String username;
    private String passwordHash;
    private Timestamp lastLogin;

    // =====================================================
    // Constructors
    // =====================================================
    public Admin() {}

    public Admin(int adminId, String username, String passwordHash, Timestamp lastLogin) {
        this.adminId = adminId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.lastLogin = lastLogin;
    }

    // =====================================================
    // Getters and Setters
    // =====================================================
    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }
}