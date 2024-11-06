module com.example.sukhdeeppizzaorder {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;
    requires java.sql;

    opens com.example.sukhdeeppizzaorder to javafx.fxml;
    exports com.example.sukhdeeppizzaorder;
}