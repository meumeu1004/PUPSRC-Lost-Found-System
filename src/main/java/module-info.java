module lostandfoundsystem {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;

    exports application;

    opens application to javafx.fxml;
    opens controller to javafx.fxml;
    opens model to javafx.base;

    exports controller;
    exports model;
    exports database;
    exports dao;
    exports util;
    opens util;
}