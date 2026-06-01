package dao;

import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    // =========================================================
    // CREATE - Insert audit log
    // =========================================================
    public boolean insertLog(int itemId,
                             String itemType,
                             String action,
                             String performedBy,
                             String oldValue,
                             String newValue) {

        String sql = """
                INSERT INTO audit_logs (
                    item_id,
                    item_type,
                    action,
                    old_value,
                    new_value,
                    performed_by,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, NOW(), NOW())
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            stmt.setString(2, itemType);
            stmt.setString(3, action);
            stmt.setString(4, oldValue);
            stmt.setString(5, newValue);
            stmt.setString(6, performedBy);

            return stmt.executeUpdate() > 0;

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // READ - All logs
    // =========================================================
    public List<String> getAllLogs() {

        List<String> logs = new ArrayList<>();

        String sql = """
                SELECT * FROM audit_logs
                ORDER BY created_at DESC
                LIMIT 200
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(formatLog(rs));
            }

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // =========================================================
    // READ - By item
    // =========================================================
    public List<String> getLogsByItem(int itemId, String itemType) {

        List<String> logs = new ArrayList<>();

        String sql = """
                SELECT * FROM audit_logs
                WHERE item_id = ? AND item_type = ?
                ORDER BY created_at DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            stmt.setString(2, itemType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(formatLog(rs));
                }
            }

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // =========================================================
    // HELPER - Format log for UI display
    // =========================================================
    private String formatLog(ResultSet rs) throws SQLException {

        return "[" + rs.getTimestamp("created_at") + "] "
                + rs.getString("item_type") + " ID "
                + rs.getInt("item_id") + " | "
                + rs.getString("action") + " | "
                + "By: " + rs.getString("performed_by");
    }
}