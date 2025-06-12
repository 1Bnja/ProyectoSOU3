module org.example.proyectoso {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.proyectoso to javafx.fxml;
    exports org.example.proyectoso;
}