package dao;

import database.DBConnection;
import model.FoundItem;
import model.LostItem;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FoundItemDAO {

    // =========================================================
    // READ - ACTIVE (Dashboard default load)
    // =========================================================
    public List<FoundItem> getAllActive() {
        return filter(null, null, null, "newest");
    }

    public List<FoundItem> filter(String keyword, String category, String status, String sortBy) {

        List<FoundItem> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT * FROM found_items
                WHERE record_status = 'Active'
                """);

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                    AND (
                        LOWER(item_name)      LIKE ?
                        OR LOWER(description) LIKE ?
                        OR LOWER(color)       LIKE ?
                        OR CAST(id AS TEXT)   LIKE ?
                    )
                    """);
        }

        if (category != null && !category.isBlank()) {
            sql.append("AND category = ? \n");
        }

        if (status != null && !status.isBlank()) {
            sql.append("AND item_status = ? \n");
        }

        sql.append(switch (sortBy == null ? "newest" : sortBy) {
            case "oldest" -> "ORDER BY created_at ASC\n";
            case "name_asc" -> "ORDER BY LOWER(item_name) ASC\n";
            case "name_desc" -> "ORDER BY LOWER(item_name) DESC\n";
            default -> "ORDER BY created_at DESC\n";
        });

        sql.append("LIMIT 300");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
            }

            if (category != null && !category.isBlank()) {
                stmt.setString(idx++, category);
            }

            if (status != null && !status.isBlank()) {
                stmt.setString(idx++, status);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(map(rs));
            }

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    // =========================================================
    // READ - ARCHIVED
    // =========================================================
    public List<FoundItem> getAllArchived() {

        List<FoundItem> items = new ArrayList<>();

        String sql = """
                SELECT * FROM found_items
                WHERE record_status = 'Archived'
                ORDER BY created_at DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) items.add(map(rs));

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    // =========================================================
    // READ - SOFT DELETED (Recycle Bin)
    // =========================================================
    public List<FoundItem> getDeleted() {

        List<FoundItem> items = new ArrayList<>();

        String sql = """
                SELECT * FROM found_items
                WHERE record_status = 'Deleted'
                ORDER BY created_at DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) items.add(map(rs));

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    // =========================================================
    // COUNTER FOR STATS
    // =========================================================

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM found_items WHERE record_status = 'Active'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countUnclaimed() {
        String sql = "SELECT COUNT(*) FROM found_items WHERE record_status = 'Active' AND item_status = 'Unclaimed'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =========================================================
    // CREATE
    // =========================================================
    public boolean insert(FoundItem item) {

        String sql = """
                INSERT INTO found_items (
                    item_name, category, description, color, date_found,
                    finder_name, finder_contact_num, finder_contact_email,
                    image_path, item_status, record_status, archived_reason
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Unclaimed', 'Active', NULL)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getColor());
            stmt.setObject(5, item.getDateFound());      // Fix 3: already a formatted String
            stmt.setString(6, item.getFinderName());
            stmt.setString(7, item.getFinderContactNum());
            stmt.setString(8, item.getFinderContactEmail());
            stmt.setString(9, item.getImagePath());

            return stmt.executeUpdate() > 0;

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // UPDATE
    // =========================================================
    public boolean update(FoundItem item) {

        String sql = """
                UPDATE found_items SET
                    item_name = ?,
                    category = ?,
                    description = ?,
                    color = ?,
                    date_found = ?,
                    finder_name = ?,
                    finder_contact_num = ?,
                    finder_contact_email = ?,
                    image_path = ?
                WHERE id = ?
                AND record_status != 'Deleted'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getColor());
            stmt.setObject(5, item.getDateFound());
            stmt.setString(6, item.getFinderName());
            stmt.setString(7, item.getFinderContactNum());
            stmt.setString(8, item.getFinderContactEmail());
            stmt.setString(9, item.getImagePath());
            stmt.setInt(10, item.getId());

            return stmt.executeUpdate() > 0;

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // STATUS METHODS
    // =========================================================
    public boolean markClaimed(int id) {
        return updateStatus(id, "Claimed");
    }

    public boolean archive(int id, String reason) {
        String sql = """
            
                UPDATE found_items
            SET record_status   = 'Archived',
                archived_reason = ?,
                archived_at     = NOW()
            WHERE id = ?
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
            return
    false;
        }

    public boolean delete(int id) {
        return updateRecordStatus(id, "Deleted");
    }

    public boolean restore(int id) {
        String sql = "UPDATE found_items " +
                "SET record_status   = 'Active', " +
                "    archived_reason = NULL, " +
                "    archived_at     = NULL " +
                "WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
            return false;
        }

    //========================================
    // FILTERS FOR ARCHIVED
    //========================================
    public List<FoundItem> filterArchived(String keyword, String category,
                                         String status, String sortBy) {
        List<FoundItem> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT * FROM found_items
            WHERE record_status = 'Archived'
            """);

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                AND (
                    LOWER(item_name)      LIKE ?
                    OR LOWER(description) LIKE ?
                    OR LOWER(color)       LIKE ?
                    OR CAST(id AS TEXT)   LIKE ?
                )
                """);
        }
        if (category != null && !category.isBlank()) {
            sql.append("AND category = ? \n");
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND item_status = ? \n");
        }
        sql.append(switch (sortBy == null ? "newest" : sortBy) {
            case "oldest"    -> "ORDER BY created_at ASC\n";
            case "name_asc"  -> "ORDER BY LOWER(item_name) ASC\n";
            case "name_desc" -> "ORDER BY LOWER(item_name) DESC\n";
            default          -> "ORDER BY created_at DESC\n";
        });
        sql.append("LIMIT 300");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
            }
            if (category != null && !category.isBlank()) {
                stmt.setString(idx++, category);
            }
            if (status != null && !status.isBlank()) {
                stmt.setString(idx++, status);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(map(rs));
            }

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private boolean updateStatus(int id, String status) {
        String sql = "UPDATE found_items SET item_status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean updateRecordStatus(int id, String status) {
        String sql = "UPDATE found_items SET record_status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;

        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================================================
    // Auto Archive
    // =======================================================

    public void autoArchiveExpired(int days, String reason) {
        String sql = "UPDATE found_items " +
                "SET record_status = 'Archived', " +
                "    archived_reason = ?, " +
                "    archived_at = NOW() " +
                "WHERE record_status = 'Active' " +
                "  AND item_status = 'Unresolved' " +
                "  AND created_at <= NOW() - (? * INTERVAL '1 day')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setInt(2, days);
            stmt.executeUpdate();
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========================================================
    // Delete
    // =======================================================
    public void autoDeleteExpired(int days) {
        String sql = "UPDATE found_items " +
                "SET record_status = 'Deleted' " +
                "WHERE record_status = 'Archived' " +
                "  AND archived_at <= NOW() - (? * INTERVAL '1 day')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            stmt.executeUpdate();
        } catch (DBConnection.NoConnectionException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private FoundItem map(ResultSet rs) throws SQLException {
        return new FoundItem(
                rs.getInt("id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getString("image_path"),
                rs.getString("record_status"),
                rs.getString("item_status"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getString("archived_reason"),
                rs.getObject("archived_at", LocalDateTime.class),
                rs.getString("finder_name"),
                rs.getString("finder_contact_num"),
                rs.getString("finder_contact_email"),
                rs.getObject("date_found", LocalDate.class)
        );
    }

    }
