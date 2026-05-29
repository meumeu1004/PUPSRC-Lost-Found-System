package model;

    public class FoundItem extends Item {
        private String finderName;
        private String finderContactNum;
        private String finderContactEmail;
        private String dateFound;
        private String itemStatus;     // Claimed, Unclaimed

        public FoundItem (int id, String itemName, String category, String description,
                         String color, String dateReported, String imagePath, String recordStatus,
                         String itemStatus, String finderName, String finderContactNum,
                         String finderContactEmail, String dateFound) {

            super(id, itemName, category, description, color, dateReported, imagePath, recordStatus);
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

        public String getDateFound() {
            return dateFound; }

        public String getItemStatus() {
            return itemStatus; }

        @Override
        public String getType() {
            return "Found"; }

    }