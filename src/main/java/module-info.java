module com.example.covid19datavisualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;

    requires io.github.cdimascio.dotenv.java;
    requires mysql.connector.j;

    opens com.example.covid19datavisualizer to javafx.fxml;
    exports com.example.covid19datavisualizer;
}