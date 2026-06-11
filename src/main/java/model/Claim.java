package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Claim {

    private int claimId;
    private int foundItemId;

    private String claimantName;
    private String studentId;
    private String claimantContactNum;
    private String claimantContactEmail;

    private String proofImagePath;
    private LocalDate claimDate;

    private String verifiedBy;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // =====================================================
    // DEFAULT CONSTRUCTOR (needed for JavaFX / frameworks)
    // =====================================================
    public Claim() {}

    // =====================================================
    // FULL CONSTRUCTOR (recommended for DAO mapping)
    // =====================================================
    public Claim(int claimId, int foundItemId,
                 String claimantName,
                 String studentId,
                 String claimantContactNum,
                 String claimantContactEmail,
                 String proofImagePath,
                 LocalDate claimDate,
                 String verifiedBy,
                 String remarks,
                 LocalDateTime createdAt,
                 LocalDateTime updatedAt) {

        this.claimId = claimId;
        this.foundItemId = foundItemId;
        this.claimantName = claimantName;
        this.studentId = studentId;
        this.claimantContactNum = claimantContactNum;
        this.claimantContactEmail = claimantContactEmail;
        this.proofImagePath = proofImagePath;
        this.claimDate = claimDate;
        this.verifiedBy = verifiedBy;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public int getFoundItemId() { return foundItemId; }
    public void setFoundItemId(int foundItemId) { this.foundItemId = foundItemId; }

    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId;}

    public String getClaimantContactNum() { return claimantContactNum; }
    public void setClaimantContactNum(String claimantContactNum) { this.claimantContactNum = claimantContactNum; }

    public String getClaimantContactEmail() { return claimantContactEmail; }
    public void setClaimantContactEmail(String claimantContactEmail) { this.claimantContactEmail = claimantContactEmail; }

    public String getProofImagePath() { return proofImagePath; }
    public void setProofImagePath(String proofImagePath) { this.proofImagePath = proofImagePath; }

    public LocalDate getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDate claimDate) { this.claimDate = claimDate; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}