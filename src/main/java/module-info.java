module com.example.malanin {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    opens com.example.malanin to javafx.fxml;
    exports com.example.malanin;
}