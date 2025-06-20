package org.example.proyectoso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.proyectoso.models.Proceso;
import javafx.scene.control.TableRow;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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

    // Lista observable de procesos y colores disponibles
    private ObservableList<Proceso> procesos;
    private final List<Color> coloresDisponibles = new ArrayList<>(Arrays.asList(
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.BROWN
    ));

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

    // Control de simulación
    private Thread simulacionThread;
    private volatile boolean corriendo = false;
    private volatile boolean pausado = false;

    private Rectangle[][] celdasGantt;

    /** Delay between simulation steps in milliseconds */
    private static long STEP_DELAY_MS = 200;

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
        procesos = FXCollections.observableArrayList();
        tablaProcesos.setItems(procesos);

        // Colorear filas según el color del proceso
        tablaProcesos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Proceso item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getColor() != null) {
                    Color c = item.getColor();
                    String rgb = String.format("rgb(%d,%d,%d)",
                            (int) (c.getRed() * 255),
                            (int) (c.getGreen() * 255),
                            (int) (c.getBlue() * 255));
                    setStyle("-fx-background-color: " + rgb + ";");
                } else {
                    setStyle("");
                }
            }
        });

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

    private void prepararGantt(int tiempoTotal) {
        gridGantt.getChildren().clear();
        celdasGantt = new Rectangle[procesos.size()][tiempoTotal];

        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        for (int fila = 0; fila < procesos.size(); fila++) {
            for (int col = 0; col < tiempoTotal; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);
                bloque.setFill(Color.TRANSPARENT);
                gridGantt.add(bloque, col, fila + 1);
                celdasGantt[fila][col] = bloque;
            }
        }
    }

    private void pintarCelda(int fila, int columna, Color color) {
        if (fila >= 0 && fila < celdasGantt.length &&
                columna >= 0 && columna < celdasGantt[0].length) {
            Rectangle r = celdasGantt[fila][columna];
            if (r != null) {
                r.setFill(color);
            }
        }
    }

    private void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ejecutarSimulacion() {
        List<Proceso> listaProcesos = new ArrayList<>(procesos);
        if (listaProcesos.isEmpty()) {
            return;
        }

        Map<Proceso, Integer> restantes = new HashMap<>();
        Map<Proceso, Integer> filaMapa = new HashMap<>();
        for (int i = 0; i < listaProcesos.size(); i++) {
            Proceso p = listaProcesos.get(i);
            restantes.put(p, p.getDuracion());
            filaMapa.put(p, i);
        }

        int tiempoTotal = listaProcesos.stream()
                .mapToInt(p -> p.getTiempoLlegada() + p.getDuracion())
                .max().orElse(0);

        int quantum = 1;
        try {
            quantum = Integer.parseInt(txtQuantum.getText());
            if (quantum <= 0) quantum = 1;
        } catch (Exception ignored) {}

        String algoritmo = comboAlgoritmo.getValue();

        int tiempoActual = 0;
        List<Proceso> pendientes = new ArrayList<>(listaProcesos);
        List<Proceso> colaListos = new ArrayList<>();

        Platform.runLater(() -> prepararGantt(tiempoTotal));

        while (corriendo && (!pendientes.isEmpty() || !colaListos.isEmpty())) {
            if (pausado) {
                esperar(100);
                continue;
            }

            // Mover procesos que ya llegaron a la cola de listos
            int finalTiempoActual = tiempoActual;
            pendientes.removeIf(p -> {
                if (p.getTiempoLlegada() <= finalTiempoActual) {
                    colaListos.add(p);
                    return true;
                }
                return false;
            });

            if (colaListos.isEmpty()) {
                tiempoActual++;
                esperar(STEP_DELAY_MS);
                continue;
            }

            Proceso actual;
            if ("Round Robin".equalsIgnoreCase(algoritmo)) {
                actual = colaListos.remove(0);
            } else { // SJF
                actual = colaListos.stream()
                        .min(Comparator.comparingInt(restantes::get))
                        .orElse(colaListos.get(0));
                colaListos.remove(actual);
            }

            int ejecucion = "Round Robin".equalsIgnoreCase(algoritmo) ?
                    Math.min(quantum, restantes.get(actual)) : restantes.get(actual);

            for (int i = 0; i < ejecucion && corriendo; i++) {
                while (pausado) {
                    esperar(100);
                }

                int fila = filaMapa.get(actual);
                int columna = tiempoActual;
                Color color = actual.getColor() == null ? Color.LIGHTBLUE : actual.getColor();
                Platform.runLater(() -> pintarCelda(fila, columna, color));

                restantes.put(actual, restantes.get(actual) - 1);
                tiempoActual++;

                // Permitir llegada de nuevos procesos en cada unidad de tiempo
                int finalTiempoActual1 = tiempoActual;
                pendientes.removeIf(p -> {
                    if (p.getTiempoLlegada() <= finalTiempoActual1) {
                        colaListos.add(p);
                        return true;
                    }
                    return false;
                });

                esperar(STEP_DELAY_MS);
            }

            if (restantes.get(actual) > 0) {
                colaListos.add(actual);
            }
        }

        corriendo = false;
    }

    @FXML
    private void onStartClicked() {
        if (corriendo) {
            return;
        }
        try {
            long v = Long.parseLong(txtTime.getText());
            if (v > 0) {
                STEP_DELAY_MS = v;
            }
        } catch (Exception ignored) {
        }
        corriendo = true;
        pausado = false;
        simulacionThread = new Thread(this::ejecutarSimulacion);
        simulacionThread.setDaemon(true);
        simulacionThread.start();
        System.out.println("Simulación iniciada");
    }

    @FXML
    private void onPauseClicked() {
        if (corriendo) {
            pausado = !pausado;
            System.out.println(pausedLabel());
        }
    }

    @FXML
    private void onStopClicked() {
        corriendo = false;
        pausado = false;
        System.out.println("Simulación detenida");
    }

    @FXML
    private void onRetryClicked() {
        corriendo = false;
        pausado = false;
        if (simulacionThread != null) {
            simulacionThread.interrupt();
        }
        Platform.runLater(() -> prepararGantt(1));
        System.out.println("Simulación reiniciada");
    }

    private String pausedLabel() {
        return pausado ? "Simulación pausada" : "Simulación reanudada";
    }

    @FXML
    private void onStatsClicked() {
        System.out.println("Mostrando estadísticas");
    }

    @FXML
    private void onAddClicked() {
        if (procesos.size() >= 6) {
            Alert alerta = new Alert(Alert.AlertType.WARNING, "Solo se pueden agregar hasta 6 procesos.");
            alerta.showAndWait();
            return;
        }

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

        dialog.showAndWait().ifPresent(proceso -> {
            if (proceso != null) {
                Color color = coloresDisponibles.remove(0);
                proceso.setColor(color);
                procesos.add(proceso);
            }
        });
    }

    @FXML
    private void onRemoveClicked() {
        Proceso seleccionado = tablaProcesos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            procesos.remove(seleccionado);
            if (seleccionado.getColor() != null) {
                coloresDisponibles.add(seleccionado.getColor());
            }
        }
    }
}
