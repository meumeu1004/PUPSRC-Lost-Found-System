package dao;

import database.DBConnection;
import model.FoundItem;
import model.LostItem;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LostItemDAO {

    // =========================================================
    // READ - ACTIVE (Dashboard)
    // =========================================================
    public List<LostItem> getAllActive() {
        List<LostItem> items = new ArrayList<>();

        String sql = """
                SELECT * FROM lost_items
                WHERE record_status = 'Active'
                ORDER BY created_at DESC
                LIMIT 300
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
    // FIX 2 — filter() added to match FoundItemDAO.filter().
    //
    // AdminController calls lostDAO.filter(keyword, cat, stat, sort)
    // but the method didn't exist — compile error. Now it does.
    //
    // Parameters (all nullable):
    //   keyword  — searches item_name, description, color, id
    //   category — exact match e.g. "Electronics"
    //   status   — item_status: "Unresolved" or "Found"
    //   sortBy   — "newest" | "oldest" | "name_asc" | "name_desc"
    // =========================================================
    public List<LostItem> filter(String keyword, String category, String status, String sortBy) {

        List<LostItem> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT * FROM lost_items
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
    // FIX 8 — getAllArchived() added.
    //
    // AdminController.applyFilters() now calls this when
    // showingArchive = true. Previously the archive branch was
    // missing, so the archive view always showed an empty grid.
    // =========================================================
    public List<LostItem> getAllArchived() {

        List<LostItem> items = new ArrayList<>();

        String sql = """
                SELECT * FROM lost_items
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
    // READ - DELETED (Recycle Bin)
    // =========================================================
    public List<LostItem> getDeleted() {

        List<LostItem> items = new ArrayList<>();

        String sql = """
                SELECT * FROM lost_items
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
    // SEARCH (kept for direct keyword-only searches if needed)
    // =========================================================
    public List<LostItem> search(String keyword) {
        return filter(keyword, null, null, "newest");
    }


    // =========================================================
    // COUNTER FOR STATS
    // =========================================================
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM lost_items WHERE record_status = 'Active'";
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

    public int countUnresolved() {
        String sql = "SELECT COUNT(*) FROM lost_items WHERE record_status = 'Active' AND item_status = 'Unresolved'";
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
    public boolean insert(LostItem item) {

        String sql = """
                INSERT INTO lost_items (
                    item_name, category, description, color, date_lost,
                    owner_name, owner_contact_num, owner_contact_email,
                    image_path, item_status, record_status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Unresolved', 'Active')
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getColor());
            stmt.setObject(5, item.getDateLost());
            stmt.setString(6, item.getOwnerName());
            stmt.setString(7, item.getOwnerContactNum());
            stmt.setString(8, item.getOwnerContactEmail());
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
    public boolean update(LostItem item) {

        String sql = """
                UPDATE lost_items SET
                    item_name = ?,
                    category = ?,
                    description = ?,
                    color = ?,
                    date_lost = ?,
                    owner_name = ?,
                    owner_contact_num = ?,
                    owner_contact_email = ?,
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
            stmt.setObject(5, item.getDateLost());
            stmt.setString(6, item.getOwnerName());
            stmt.setString(7, item.getOwnerContactNum());
            stmt.setString(8, item.getOwnerContactEmail());
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
    // STATUS UPDATES
    // =========================================================
    public boolean markFound(int id) {

            String sql = """
                UPDATE lost_items
                SET item_status = 'Found',
                    record_status = 'Archived',
                    archived_reason = 'Marked as Found',
                    archived_at = NOW() AT TIME ZONE 'Asia/Manila'
                WHERE id = ?
                """;
        
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

    public boolean archive(int id, String reason) {
        String sql = """
            UPDATE lost_items
            SET record_status   = 'Archived',
                archived_reason = ?,
                archived_at     = NOW() AT TIME ZONE 'Asia/Manila'
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
            return false;
        }


    public boolean delete(int id) {
        return updateRecordStatus(id, "Deleted");
    }

    public boolean restore(int id) {
        String sql = "UPDATE lost_items " +
                "SET record_status   = 'Active', " +
                "    item_status     = 'Unresolved', " +
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
    public List<LostItem> filterArchived(String keyword, String category,
                                         String status, String sortBy) {
        List<LostItem> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT * FROM lost_items
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
    // PRIVATE HELPERS
    // =========================================================
    private boolean updateItemStatus(int id, String status) {
        String sql = "UPDATE lost_items SET item_status = ? WHERE id = ?";

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
        String sql = "UPDATE lost_items SET record_status = ? WHERE id = ?";

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
        String sql = "UPDATE lost_items " +
                "SET record_status = 'Archived', " +
                "    archived_reason = ?, " +
                "    archived_at = NOW() AT TIME ZONE 'Asia/Manila' " +
                "WHERE record_status = 'Active' " +
                "  AND item_status = 'Unresolved' " +
                "  AND created_at <= NOW() AT TIME ZONE 'Asia/Manila' - (? * INTERVAL '1 day')";

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

    // LostItemDAO — add after autoArchiveExpired():
    public void autoDeleteExpired(int days) {
        String sql = "UPDATE lost_items " +
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

    public List<LostItem> findCandidatesFor(FoundItem found) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM lost_items
            WHERE record_status = 'Active'
              AND item_status   = 'Unresolved'
            """);

        boolean hasCategory = found.getCategory() != null && !found.getCategory().isBlank();
        boolean hasColor    = found.getColor()    != null && !found.getColor().isBlank();

        if (hasCategory) sql.append("AND category = ? \n");
        if (hasColor)    sql.append("AND LOWER(color) = LOWER(?) \n");

        sql.append("ORDER BY created_at DESC LIMIT 500");

        List<LostItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (hasCategory) stmt.setString(idx++, found.getCategory());
            if (hasColor)    stmt.setString(idx++, found.getColor());

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

    public boolean updateArchiveReason(int id, String reason) {
        String sql = """
            UPDATE lost_items
            SET archived_reason = ?
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
            return false;
        }
    }

    // =========================================================
    // TIME HELPER
    // =========================================================
    private static final java.time.ZoneId MANILA = java.time.ZoneId.of("Asia/Manila");

    private LocalDateTime toManila(ResultSet rs, String col) throws SQLException {
        java.time.OffsetDateTime odt = rs.getObject(col, java.time.OffsetDateTime.class);
        return odt != null ? odt.atZoneSameInstant(MANILA).toLocalDateTime() : null;
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private LostItem map(ResultSet rs) throws SQLException {
        return new LostItem(
                rs.getInt("id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getString("image_path"),
                rs.getString("record_status"),
                toManila(rs, "created_at"),
                toManila(rs, "updated_at"),
                rs.getString("archived_reason"),
                toManila(rs, "archived_at"),
                rs.getString("item_status"),
                rs.getString("owner_name"),
                rs.getString("owner_contact_num"),
                rs.getString("owner_contact_email"),
                rs.getObject("date_lost", LocalDate.class)

        );
    }
}
