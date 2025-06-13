package org.example.proyectoso;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    @FXML
    private GridPane gridGantt;

    @FXML private VBox ramBox;
    @FXML private VBox discoBox;

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

        generarGanttDePrueba();
        poblarMemorias();
    }

    private void generarGanttDePrueba() {
        gridGantt.getChildren().clear();
        int tiempoTotal = 100;

        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        for (int fila = 1; fila <= 6; fila++) {
            for (int col = 0; col < tiempoTotal; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);

                if ((fila == 1 && col >= 0 && col < 5) ||
                        (fila == 2 && col >= 2 && col < 5) ||
                        (fila == 3 && col >= 5 && col < 10)) {
                    bloque.setFill(Color.LIGHTBLUE);
                } else {
                    bloque.setFill(Color.TRANSPARENT);
                }

                gridGantt.add(bloque, col, fila);
            }
        }
    }

    private void poblarMemorias() {
        ramBox.getChildren().clear();
        discoBox.getChildren().clear();

        for (int i = 0; i < 4; i++) {
            Rectangle bloque = new Rectangle(100, 30);
            bloque.setFill(Color.LIGHTGREEN);  // Bloque RAM libre
            bloque.setStroke(Color.BLACK);
            ramBox.getChildren().add(bloque);
        }

        for (int i = 0; i < 6; i++) {
            Rectangle bloque = new Rectangle(100, 30);
            bloque.setFill(Color.LIGHTGRAY);  // Bloque Disco libre
            bloque.setStroke(Color.BLACK);
            discoBox.getChildren().add(bloque);
        }
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
        System.out.println("Agregar nuevo proceso");
    }

    @FXML
    private void onRemoveClicked() {
        System.out.println("Remover proceso");
    }
}
