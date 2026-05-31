package dao;

import database.DBConnection;
import model.Admin;

import java.sql.*;

public class AdminDAO {

    // =========================================================
    // GET ADMIN BY USERNAME (use this for login — see login() below)
    // =========================================================
    public Admin getByUsername(String username) {

        String sql = """
                SELECT * FROM admin_settings
                WHERE username = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // FIX 8 — LOGIN via bcrypt (no longer compares hash in SQL)
    //
    // OLD (broken): WHERE username = ? AND password_hash = ?
    //   → plain string comparison against a bcrypt hash always fails
    //
    // NEW: fetch admin by username only, then verify in Java with
    //   BCrypt.verifyer().verify(typedPassword, storedHash).verified
    //
    // USAGE in controller:
    //   Admin admin = adminDAO.getByUsername(username);
    //   boolean ok  = adminDAO.verifyPassword(typedPassword, admin);
    //   if (ok) { adminDAO.updateLastLogin(admin.getAdminId()); }
    // =========================================================
    public boolean verifyPassword(String typedPassword, Admin admin) {

        if (admin == null || typedPassword == null) return false;

        return org.mindrot.jbcrypt.BCrypt.checkpw(
                typedPassword,
                admin.getPasswordHash()
        );
    }

    // =========================================================
    // UPDATE PASSWORD — caller must hash BEFORE calling this
    //
    // USAGE in SettingsController:
    //   String hash = BCrypt.withDefaults()
    //                       .hashToString(12, newPassword.toCharArray());
    //   adminDAO.updatePassword(admin.getAdminId(), hash);
    // =========================================================
    public boolean updatePassword(int adminId, String newPasswordHash) {

        String sql = """
                UPDATE admin_settings
                SET password_hash = ?, updated_at = NOW()
                WHERE admin_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, adminId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // UPDATE LAST LOGIN
    // =========================================================
    public boolean updateLastLogin(int adminId) {

        String sql = """
                UPDATE admin_settings
                SET last_login = NOW()
                WHERE admin_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, adminId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private Admin map(ResultSet rs) throws SQLException {

        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getTimestamp("last_login")
        );
    }
}