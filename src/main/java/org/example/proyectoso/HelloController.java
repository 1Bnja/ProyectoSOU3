package org.example.proyectoso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

        crearGridGantt();
        poblarMemorias();
    }

    private static final int TIEMPO_TOTAL = 100;
    private final List<List<Rectangle>> matrizGantt = new ArrayList<>();

    private void crearGridGantt() {
        gridGantt.getChildren().clear();
        matrizGantt.clear();

        for (int t = 0; t < TIEMPO_TOTAL; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        for (int fila = 1; fila <= 6; fila++) {
            List<Rectangle> filaRect = new ArrayList<>();
            for (int col = 0; col < TIEMPO_TOTAL; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);
                bloque.setFill(Color.TRANSPARENT);
                gridGantt.add(bloque, col, fila);
                filaRect.add(bloque);
            }
            matrizGantt.add(filaRect);
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
        ejecutarSJF();
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

    private void ejecutarSJF() {
        if (procesos.isEmpty()) {
            return;
        }

        crearGridGantt();

        List<Proceso> pendientes = new ArrayList<>(procesos);
        int tiempo = 0;

        while (!pendientes.isEmpty() && tiempo < TIEMPO_TOTAL) {
            List<Proceso> disponibles = pendientes.stream()
                    .filter(p -> p.getTiempoLlegada() <= tiempo)
                    .toList();

            if (disponibles.isEmpty()) {
                tiempo++;
                continue;
            }

            Proceso siguiente = disponibles.stream()
                    .min(Comparator.comparingInt(Proceso::getDuracion))
                    .orElse(null);

            if (siguiente == null) {
                tiempo++;
                continue;
            }

            int row = procesos.indexOf(siguiente);
            for (int i = 0; i < siguiente.getDuracion() && tiempo < TIEMPO_TOTAL; i++) {
                Rectangle r = matrizGantt.get(row).get(tiempo);
                r.setFill(siguiente.getColor());
                tiempo++;
            }

            pendientes.remove(siguiente);
        }
    }
}
