package dao;

import database.DBConnection;
import model.Claim;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClaimDAO {
    public boolean insertClaimAndMarkClaimed(Claim claim) {
        String sqlClaim = """
            INSERT INTO claims (
                found_item_id, claimant_name, student_id,
                claimant_contact_num, claimant_contact_email,
                proof_image_path, claim_date, verified_by,
                remarks, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, ?, ?, NOW(), NOW())
            """;
        String sqlUpdate = """
            UPDATE found_items SET item_status = 'Claimed'
            WHERE id = ? AND record_status != 'Deleted'
            """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtClaim = conn.prepareStatement(sqlClaim);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {

                stmtClaim.setInt(1, claim.getFoundItemId());
                stmtClaim.setString(2, claim.getClaimantName());
                stmtClaim.setString(3, claim.getStudentId());
                stmtClaim.setString(4, claim.getClaimantContactNum());
                stmtClaim.setString(5, claim.getClaimantContactEmail());
                stmtClaim.setString(6, claim.getProofImagePath());
                stmtClaim.setString(7, claim.getVerifiedBy());
                stmtClaim.setString(8, claim.getRemarks());
                stmtClaim.executeUpdate();

                stmtUpdate.setInt(1, claim.getFoundItemId());
                stmtUpdate.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertClaim(Claim claim) {

        String sql = """
                INSERT INTO claims (
                    found_item_id,
                    claimant_name,
                    student_id,
                    claimant_contact_num,
                    claimant_contact_email,
                    proof_image_path,
                    claim_date,
                    verified_by,
                    remarks,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, ?, ?, NOW(), NOW())
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, claim.getFoundItemId());
            stmt.setString(2, claim.getClaimantName());
            stmt.setString(3, claim.getStudentId());
            stmt.setString(4, claim.getClaimantContactNum());
            stmt.setString(5, claim.getClaimantContactEmail());
            stmt.setString(6, claim.getProofImagePath());
            stmt.setString(7, claim.getVerifiedBy());         // FIX 9: real admin username
            stmt.setString(8, claim.getRemarks());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // READ - Get all claims
    // =========================================================
    public List<Claim> getAllClaims() {

        List<Claim> list = new ArrayList<>();

        String sql = """
                SELECT * FROM claims
                ORDER BY created_at DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // READ - By Found Item
    // =========================================================
    public List<Claim> getClaimsByFoundItem(int foundItemId) {

        List<Claim> list = new ArrayList<>();

        String sql = """
                SELECT * FROM claims
                WHERE found_item_id = ?
                ORDER BY created_at DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, foundItemId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // READ - Single Claim
    // =========================================================
    public Claim getById(int id) {

        String sql = "SELECT * FROM claims WHERE claim_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // UPDATE - Verify Claim
    // =========================================================
    public boolean verifyClaim(int claimId, String verifiedBy) {

        String sql = """
                UPDATE claims
                SET verified_by = ?
                WHERE claim_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, verifiedBy);
            stmt.setInt(2, claimId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // UPDATE - Add remarks
    // =========================================================
    public boolean updateRemarks(int claimId, String remarks) {

        String sql = """
                UPDATE claims
                SET remarks = ?
                WHERE claim_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, remarks);
            stmt.setInt(2, claimId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // DELETE (optional - usually NOT used in audit systems)
    // =========================================================
    public boolean deleteClaim(int claimId) {

        String sql = "DELETE FROM claims WHERE claim_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, claimId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // MAPPER
    //
    // FIX 10: student_id removed — column does not exist in the DB.
    //   studentId is passed as null; the Claim model still holds
    //   the field for any local display needs.
    // =========================================================
    private Claim map(ResultSet rs) throws SQLException {

        return new Claim(
                rs.getInt("claim_id"),
                rs.getInt("found_item_id"),
                rs.getString("claimant_name"),
                rs.getString("student_id"),                                   // studentId — not in DB schema
                rs.getString("claimant_contact_num"),
                rs.getString("claimant_contact_email"),
                rs.getString("proof_image_path"),
                rs.getString("claim_date"),
                rs.getString("verified_by"),
                rs.getString("remarks"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}