package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FoundItem extends Item {
        private String finderName;
        private String finderContactNum;
        private String finderContactEmail;
        private LocalDate dateFound;
        private String itemStatus;     // Claimed, Unclaimed



        public FoundItem (int id, String itemName, String category, String description,
                          String color, String imagePath, String recordStatus,
                          String itemStatus, LocalDateTime createdAt, LocalDateTime updatedAt, String archivedReason,
                          LocalDateTime archivedAt, String finderName, String finderContactNum,
                          String finderContactEmail, LocalDate dateFound) {

            super(id, itemName, category, description, color, imagePath, recordStatus, createdAt, updatedAt, archivedReason, archivedAt);
            this.itemStatus = itemStatus;
            this.finderName = finderName;
            this.finderContactNum = finderContactNum;
            this.finderContactEmail = finderContactEmail;
            this.dateFound = dateFound;
        }

        public String getFinderName() {
            return finderName; }

        public String getFinderContactNum() {
            return finderContactNum; }

        public String getFinderContactEmail() {
            return finderContactEmail; }

        public LocalDate getDateFound() {
            return dateFound; }

        public String getItemStatus() {
            return itemStatus; }


        @Override
        public String getType() {
            return "Found"; }

    }