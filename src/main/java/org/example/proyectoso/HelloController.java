package org.example.proyectoso;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.proyectoso.memoria.Memoria;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;
import org.example.proyectoso.models.CPU;
import org.example.proyectoso.planificacion.ManejoProcesos;
import org.example.proyectoso.planificacion.Planificacion;
import org.example.proyectoso.planificacion.SJF;

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

    // Tabla de colas de procesos
    @FXML
    private TableView<EstadoRow> tablaColas;
    @FXML
    private TableColumn<EstadoRow, String> colNuevo;
    @FXML
    private TableColumn<EstadoRow, String> colListo;
    @FXML
    private TableColumn<EstadoRow, String> colEspera;
    @FXML
    private TableColumn<EstadoRow, String> colTerminado;

    // Objetos principales de la simulación
    private CPU cpu;
    private Memoria memoria;
    private ManejoProcesos manejoProcesos;
    private Planificacion planificador;
    private Timeline uiUpdater;

    /**
     * Modelo para la tabla de colas de procesos
     */
    public static class EstadoRow {
        private final SimpleStringProperty nuevo = new SimpleStringProperty("");
        private final SimpleStringProperty listo = new SimpleStringProperty("");
        private final SimpleStringProperty espera = new SimpleStringProperty("");
        private final SimpleStringProperty terminado = new SimpleStringProperty("");

        public String getNuevo() { return nuevo.get(); }
        public void setNuevo(String v) { nuevo.set(v); }
        public SimpleStringProperty nuevoProperty() { return nuevo; }

        public String getListo() { return listo.get(); }
        public void setListo(String v) { listo.set(v); }
        public SimpleStringProperty listoProperty() { return listo; }

        public String getEspera() { return espera.get(); }
        public void setEspera(String v) { espera.set(v); }
        public SimpleStringProperty esperaProperty() { return espera; }

        public String getTerminado() { return terminado.get(); }
        public void setTerminado(String v) { terminado.set(v); }
        public SimpleStringProperty terminadoProperty() { return terminado; }
    }

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

        // Configurar tabla de colas
        colNuevo.setCellValueFactory(data -> data.getValue().nuevoProperty());
        colListo.setCellValueFactory(data -> data.getValue().listoProperty());
        colEspera.setCellValueFactory(data -> data.getValue().esperaProperty());
        colTerminado.setCellValueFactory(data -> data.getValue().terminadoProperty());
        tablaColas.setItems(FXCollections.observableArrayList(new EstadoRow()));

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

    // ------ Actualización de interfaz ------
    private void startUiUpdater() {
        if (uiUpdater != null) {
            uiUpdater.stop();
        }
        uiUpdater = new Timeline(new KeyFrame(Duration.millis(500), e -> updateViews()));
        uiUpdater.setCycleCount(Timeline.INDEFINITE);
        uiUpdater.play();
    }

    private void stopUiUpdater() {
        if (uiUpdater != null) {
            uiUpdater.stop();
            uiUpdater = null;
        }
    }

    private void updateViews() {
        updateTablaColas();
        actualizarGantt();
    }

    private void updateTablaColas() {
        if (manejoProcesos == null) return;

        java.util.List<Proceso> todos = new java.util.ArrayList<>();
        todos.addAll(manejoProcesos.obtenerTodosLosProcesos());
        todos.addAll(manejoProcesos.getProcesosEnEjecucionList());
        todos.addAll(manejoProcesos.getProcesosCompletadosList());

        java.util.function.Function<EstadoProceso, String> joiner = estado ->
                todos.stream()
                        .filter(p -> p.getEstado() == estado)
                        .map(Proceso::getNombre)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

        EstadoRow row;
        if (tablaColas.getItems().isEmpty()) {
            row = new EstadoRow();
            tablaColas.getItems().add(row);
        } else {
            row = tablaColas.getItems().get(0);
        }

        row.setNuevo(joiner.apply(EstadoProceso.NUEVO));
        row.setListo(joiner.apply(EstadoProceso.LISTO));
        row.setEspera(joiner.apply(EstadoProceso.ESPERANDO));
        row.setTerminado(joiner.apply(EstadoProceso.TERMINADO));
    }

    private void actualizarGantt() {
        if (manejoProcesos == null) return;

        int tiempoTotal = 100;
        gridGantt.getChildren().clear();

        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        java.util.List<Proceso> procesos = tablaProcesos.getItems();
        int fila = 1;
        for (Proceso p : procesos) {
            int ejecutado = p.getTiempoEjecutado() / 10;
            for (int col = 0; col < tiempoTotal; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);
                bloque.setFill(col < ejecutado ? Color.LIGHTBLUE : Color.TRANSPARENT);
                gridGantt.add(bloque, col, fila);
            }
            fila++;
        }
    }

    @FXML
    private void onStartClicked() {
        if (cpu == null) {
            cpu = new CPU(2);
        }
        if (memoria == null) {
            memoria = new Memoria(1024);
        }
        if (manejoProcesos == null) {
            manejoProcesos = new ManejoProcesos();
        }

        manejoProcesos.reiniciar();
        manejoProcesos.agregarProcesos(new java.util.ArrayList<>(tablaProcesos.getItems()));

        String algoritmo = comboAlgoritmo.getValue();
        planificador = new SJF(); // Único implementado actualmente
        planificador.setCpu(cpu);
        manejoProcesos.setPlanificador(planificador);

        manejoProcesos.iniciarEjecucion();
        startUiUpdater();

        System.out.println("Simulación iniciada");
    }

    @FXML
    private void onPauseClicked() {
        if (manejoProcesos != null) {
            manejoProcesos.pausarEjecucion();
        }
        if (planificador != null) {
            planificador.pausar();
        }
        System.out.println("Simulación pausada");
    }

    @FXML
    private void onStopClicked() {
        if (manejoProcesos != null) {
            manejoProcesos.detenerEjecucion();
        }
        if (planificador != null) {
            planificador.detener();
        }
        stopUiUpdater();
        System.out.println("Simulación detenida");
    }

    @FXML
    private void onRetryClicked() {
        onStopClicked();
        if (manejoProcesos != null) {
            manejoProcesos.reiniciar();
        }
        if (cpu != null) {
            cpu.reiniciar();
        }
        if (memoria != null) {
            memoria.reiniciar();
        }
        onStartClicked();
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
