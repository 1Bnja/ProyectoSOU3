package org.example.proyectoso;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    @FXML
    private ComboBox<String> comboAlgoritmo;
    @FXML
    private TextField txtQuantum;
    @FXML
    private TextField txtTime;
    @FXML
    private Button btnStart, btnPause, btnStop, btnRetry, btnStats, btnAdd, btnRemove;

    @FXML
    private TableView<ObservableList<String>> tablaProcesos;
    @FXML
    private TableColumn<ObservableList<String>, String> colProceso;
    @FXML
    private TableColumn<ObservableList<String>, String> colLlegada;
    @FXML
    private TableColumn<ObservableList<String>, String> colBurst;
    @FXML
    private TableColumn<ObservableList<String>, String> colMemoria;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup ComboBox
        comboAlgoritmo.getItems().addAll("SJF", "Round Robin");
        comboAlgoritmo.setValue("SJF");

        comboAlgoritmo.setOnAction(event -> {
            String seleccionado = comboAlgoritmo.getValue();
            System.out.println("Algoritmo seleccionado: " + seleccionado);
        });

        // Configurar columnas con índice
        colProceso.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(0)));
        colLlegada.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(1)));
        colBurst.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(2)));
        colMemoria.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(3)));

        // Datos de prueba
        ObservableList<ObservableList<String>> datos = FXCollections.observableArrayList();
        datos.add(FXCollections.observableArrayList("A", "0", "5", "128"));
        datos.add(FXCollections.observableArrayList("B", "2", "3", "256"));
        datos.add(FXCollections.observableArrayList("C", "4", "6", "512"));
        tablaProcesos.setItems(datos);
    }

    @FXML
    private void onStartClicked() {
        System.out.println("Simulación iniciada");
    }

    @FXML
    private void onPauseClicked() {
        System.out.println("Simulación pausada");
    }

    @FXML
    private void onStopClicked() {
        System.out.println("Simulación detenida");
    }

    @FXML
    private void onRetryClicked() {
        System.out.println("Simulación reiniciada");
    }

    @FXML
    private void onStatsClicked() {
        System.out.println("Mostrando estadísticas");
    }

    @FXML
    private void onAddClicked() {
        // Lógica para agregar un nuevo proceso
        System.out.println("Agregar nuevo proceso");
    }
    @FXML
    private void onRemoveClicked() {
        System.out.println("Remover proceso");
    }

}
