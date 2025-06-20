package org.example.proyectoso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.proyectoso.models.Proceso;

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
    private TableView<Proceso> tablaProcesos;
    @FXML
    private TableColumn<Proceso, String> colProceso;
    @FXML
    private TableColumn<Proceso, Integer> colLlegada;
    @FXML
    private TableColumn<Proceso, Integer> colBurst;
    @FXML
    private TableColumn<Proceso, Integer> colMemoria;

    @FXML
    private GridPane gridGantt;

    @FXML private VBox ramBox;
    @FXML private VBox discoBox;

    // Matriz visual del Gantt
    private Rectangle[][] matrizGantt;
    private Timeline timeline;
    private int tiempoActual;

    // Tabla de colas de procesos
    @FXML
    private TableView<?> tablaColas;
    @FXML
    private TableColumn<?, ?> colNuevo;
    @FXML
    private TableColumn<?, ?> colListo;
    @FXML
    private TableColumn<?, ?> colEspera;
    @FXML
    private TableColumn<?, ?> colTerminado;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup ComboBox
        comboAlgoritmo.getItems().addAll("SJF", "Round Robin");
        comboAlgoritmo.setValue("SJF");

        comboAlgoritmo.setOnAction(event -> {
            String seleccionado = comboAlgoritmo.getValue();
            System.out.println("Algoritmo seleccionado: " + seleccionado);
        });

        // Configurar columnas usando propiedades del Proceso
        colProceso.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombre()));
        colLlegada.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTiempoLlegada()).asObject());
        colBurst.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getDuracion()).asObject());
        colMemoria.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTamanoMemoria()).asObject());

        // Lista de procesos
        tablaProcesos.setItems(FXCollections.observableArrayList());

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

    private void ejecutarSJF() {
        ObservableList<Proceso> procesos = tablaProcesos.getItems();
        if (procesos.isEmpty()) {
            return;
        }

        var ordenados = procesos.stream()
                .sorted(java.util.Comparator.comparingInt(Proceso::getDuracion))
                .toList();

        int tiempoTotal = ordenados.stream()
                .mapToInt(Proceso::getDuracion)
                .sum();

        gridGantt.getChildren().clear();
        for (int t = 0; t < tiempoTotal; t++) {
            Label lbl = new Label("t" + t);
            lbl.setPrefSize(30, 30);
            lbl.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(lbl, t, 0);
        }

        matrizGantt = new Rectangle[ordenados.size()][tiempoTotal];
        for (int fila = 0; fila < ordenados.size(); fila++) {
            for (int col = 0; col < tiempoTotal; col++) {
                Rectangle r = new Rectangle(30, 30);
                r.setStroke(Color.GRAY);
                r.setFill(Color.TRANSPARENT);
                matrizGantt[fila][col] = r;
                gridGantt.add(r, col, fila + 1);
            }
        }

        tiempoActual = 0;
        final int[] indiceProceso = {0};
        final int[] restante = {ordenados.get(0).getDuracion()};
        Color[] colores = {Color.LIGHTBLUE, Color.LIGHTGREEN, Color.LIGHTPINK,
                Color.LIGHTYELLOW, Color.LIGHTCORAL, Color.LIGHTSEAGREEN};

        timeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            if (tiempoActual >= tiempoTotal) {
                timeline.stop();
                return;
            }

            Rectangle rect = matrizGantt[indiceProceso[0]][tiempoActual];
            rect.setFill(colores[indiceProceso[0] % colores.length]);

            restante[0]--;
            tiempoActual++;

            if (restante[0] == 0 && indiceProceso[0] < ordenados.size() - 1) {
                indiceProceso[0]++;
                restante[0] = ordenados.get(indiceProceso[0]).getDuracion();
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void onStartClicked() {
        if (timeline == null || timeline.getStatus() == Animation.Status.STOPPED) {
            ejecutarSJF();
        } else {
            timeline.play();
        }
        System.out.println("Simulación iniciada");
    }

    @FXML
    private void onPauseClicked() {
        if (timeline != null) {
            timeline.pause();
        }
        System.out.println("Simulación pausada");
    }

    @FXML
    private void onStopClicked() {
        if (timeline != null) {
            timeline.stop();
        }
        System.out.println("Simulación detenida");
    }

    @FXML
    private void onRetryClicked() {
        if (timeline != null) {
            timeline.stop();
        }
        ejecutarSJF();
        System.out.println("Simulación reiniciada");
    }

    @FXML
    private void onStatsClicked() {
        System.out.println("Mostrando estadísticas");
    }

    @FXML
    private void onAddClicked() {
        Dialog<Proceso> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Proceso");

        ButtonType addButton = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nombreField = new TextField();
        TextField llegadaField = new TextField();
        TextField burstField = new TextField();
        TextField memoriaField = new TextField();

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Llegada:"), 0, 1);
        grid.add(llegadaField, 1, 1);
        grid.add(new Label("Burst:"), 0, 2);
        grid.add(burstField, 1, 2);
        grid.add(new Label("Memoria:"), 0, 3);
        grid.add(memoriaField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                try {
                    String nombre = nombreField.getText();
                    int llegada = Integer.parseInt(llegadaField.getText());
                    int burst = Integer.parseInt(burstField.getText());
                    int memoria = Integer.parseInt(memoriaField.getText());
                    return new Proceso(nombre, burst, memoria, llegada);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(proceso -> tablaProcesos.getItems().add(proceso));
    }

    @FXML
    private void onRemoveClicked() {
        Proceso seleccionado = tablaProcesos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            tablaProcesos.getItems().remove(seleccionado);
        }
    }
}
