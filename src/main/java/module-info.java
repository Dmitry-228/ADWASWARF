module com.example.malanin {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires com.github.librepdf.openpdf;

    opens com.example.malanin to javafx.fxml;
    exports com.example.malanin;
}