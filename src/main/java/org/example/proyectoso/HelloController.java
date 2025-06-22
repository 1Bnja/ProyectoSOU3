package org.example.proyectoso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.proyectoso.models.*;
import org.example.proyectoso.planificacion.*;
import javafx.scene.control.TableRow;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @FXML private AnchorPane ramContainer;
    @FXML private AnchorPane discoContainer;

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

    // Sistema de procesamiento paralelo
    private CPU cpu;
    private ManejoProcesos manejoProcesos;
    private Planificacion planificador;

    // Control de simulación
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean corriendo = false;
    private volatile boolean pausado = false;
    private int tiempoActual = 0;

    private Rectangle[][] celdasGantt;
    private Map<Proceso, Integer> filaMapa = new HashMap<>();
    private int nextFila = 0;

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

        // Inicializar sistema de procesamiento
        inicializarSistemaProcesamiento();

        generarGanttVacio();
        poblarMemorias();
    }

    private void inicializarSistemaProcesamiento() {
        // Crear CPU con 6 cores
        cpu = new CPU(6);

        // Crear manejador de procesos
        manejoProcesos = new ManejoProcesos();

        // Configurar planificador inicial
        configurarPlanificador();

        // Crear executor para actualizaciones de interfaz
        scheduledExecutor = Executors.newScheduledThreadPool(2);

        System.out.println("🔧 Sistema de procesamiento inicializado");
    }

    private void configurarPlanificador() {
        String algoritmo = comboAlgoritmo.getValue();

        switch (algoritmo) {
            case "SJF":
                planificador = new SJF();
                cpu.setAlgoritmo(CPU.TipoAlgoritmo.SJF);
                break;
            case "Round Robin":
                planificador = new RoundRobin(); // Necesitarás crear esta clase
                cpu.setAlgoritmo(CPU.TipoAlgoritmo.ROUND_ROBIN);
                try {
                    int quantum = Integer.parseInt(txtQuantum.getText());
                    cpu.setQuantumRoundRobin(quantum);
                } catch (Exception e) {
                    cpu.setQuantumRoundRobin(100); // Quantum por defecto
                }
                break;
        }

        manejoProcesos.setPlanificador(planificador);
    }

    private void generarGanttVacio() {
        gridGantt.getChildren().clear();
        int tiempoTotal = 100;

        celdasGantt = new Rectangle[6][tiempoTotal]; // 6 cores máximo

        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        for (int core = 0; core < 6; core++) {
            // Agregar etiqueta de core
            Label coreLabel = new Label("C" + core);
            coreLabel.setPrefSize(30, 30);
            coreLabel.setStyle("-fx-border-color: gray; -fx-alignment: center; -fx-background-color: lightgray;");
            gridGantt.add(coreLabel, 0, core + 1);

            for (int col = 1; col < tiempoTotal; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);
                bloque.setFill(Color.TRANSPARENT);
                gridGantt.add(bloque, col, core + 1);
                celdasGantt[core][col-1] = bloque;
            }
        }
    }

    private void poblarMemorias() {
        ramContainer.getChildren().clear();
        discoContainer.getChildren().clear();
        System.out.println("Contenedores de memoria inicializados");
    }

    // Método auxiliar para agregar elementos a la RAM dinámicamente
    public void agregarProcesoRAM(Proceso proceso, double x, double y, double width, double height) {
        Rectangle rect = new Rectangle(width, height);
        rect.setFill(proceso.getColor() != null ? proceso.getColor() : Color.LIGHTBLUE);
        rect.setStroke(Color.BLACK);
        rect.setX(x);
        rect.setY(y);
        ramContainer.getChildren().add(rect);
    }

    // Método auxiliar para agregar elementos al Disco dinámicamente
    public void agregarProcesoDisco(Proceso proceso, double x, double y, double width, double height) {
        Rectangle rect = new Rectangle(width, height);
        rect.setFill(proceso.getColor() != null ? proceso.getColor() : Color.LIGHTGRAY);
        rect.setStroke(Color.BLACK);
        rect.setX(x);
        rect.setY(y);
        discoContainer.getChildren().add(rect);
    }

    // Método para limpiar la memoria
    public void limpiarMemoria() {
        ramContainer.getChildren().clear();
        discoContainer.getChildren().clear();
    }

    private void prepararGantt() {
        if (celdasGantt == null) {
            generarGanttVacio();
            return;
        }

        for (Rectangle[] fila : celdasGantt) {
            for (Rectangle celda : fila) {
                if (celda != null) {
                    celda.setFill(Color.TRANSPARENT);
                }
            }
        }
    }

    private void pintarCeldaCore(int coreId, int tiempoColumna, Color color) {
        Platform.runLater(() -> {
            if (coreId >= 0 && coreId < celdasGantt.length &&
                    tiempoColumna >= 0 && tiempoColumna < celdasGantt[0].length) {
                Rectangle r = celdasGantt[coreId][tiempoColumna];
                if (r != null) {
                    r.setFill(color);
                }
            }
        });
    }

    /**
     * Nueva simulación paralela - simulación por unidades de tiempo CPU Burst
     */
    private void ejecutarSimulacionParalela() {
        List<Proceso> listaProcesos = new ArrayList<>(procesos);

        // Asignar filas de visualización a procesos
        filaMapa.clear();
        nextFila = 0;
        for (Proceso p : listaProcesos) {
            if (!filaMapa.containsKey(p)) {
                filaMapa.put(p, nextFila++);
            }
        }

        // Cola de procesos por estado
        List<Proceso> procesosPendientes = new ArrayList<>(listaProcesos);
        List<Proceso> procesosListos = new ArrayList<>();
        Map<Integer, Proceso> procesosEnCores = new HashMap<>(); // core -> proceso
        Map<Proceso, Integer> tiempoRestanteProceso = new HashMap<>();

        // Inicializar tiempos restantes
        for (Proceso p : listaProcesos) {
            tiempoRestanteProceso.put(p, p.getDuracion());
        }

        Platform.runLater(this::prepararGantt);

        // Thread principal de simulación
        Thread simulacionThread = new Thread(() -> {
            try {
                tiempoActual = 0;

                while (corriendo && (!procesosPendientes.isEmpty() || !procesosListos.isEmpty() || !procesosEnCores.isEmpty())) {

                    while (pausado && corriendo) {
                        Thread.sleep(100);
                    }

                    if (!corriendo) break;

                    // 1. Verificar llegadas de procesos
                    procesosPendientes.removeIf(proceso -> {
                        if (proceso.getTiempoLlegada() <= tiempoActual) {
                            procesosListos.add(proceso);
                            proceso.setEstado(EstadoProceso.LISTO);
                            System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó");
                            return true;
                        }
                        return false;
                    });

                    // 2. Asignar procesos a cores libres usando SJF
                    if (!procesosListos.isEmpty()) {
                        // Ordenar por SJF (menor duración primero)
                        procesosListos.sort((p1, p2) -> Integer.compare(
                                tiempoRestanteProceso.get(p1),
                                tiempoRestanteProceso.get(p2)
                        ));

                        for (int coreId = 0; coreId < 6 && !procesosListos.isEmpty(); coreId++) {
                            if (!procesosEnCores.containsKey(coreId)) {
                                Proceso proceso = procesosListos.remove(0);
                                procesosEnCores.put(coreId, proceso);
                                proceso.setEstado(EstadoProceso.EJECUTANDO);
                                System.out.println("🔧 t=" + tiempoActual + ": Core-" + coreId + " ejecuta Proceso " + proceso.getId());
                            }
                        }
                    }

                    // 3. Ejecutar procesos en cores por 1 unidad de tiempo
                    List<Integer> coresALiberar = new ArrayList<>();
                    for (Map.Entry<Integer, Proceso> entry : procesosEnCores.entrySet()) {
                        int coreId = entry.getKey();
                        Proceso proceso = entry.getValue();

                        // Visualizar en Gantt
                        final int finalTiempo = tiempoActual;
                        final int finalCore = coreId;
                        Platform.runLater(() -> {
                            Color color = proceso.getColor() != null ? proceso.getColor() : Color.LIGHTBLUE;
                            pintarCeldaCore(finalCore, finalTiempo, color);
                        });

                        // Reducir tiempo restante
                        int tiempoRestante = tiempoRestanteProceso.get(proceso) - 1;
                        tiempoRestanteProceso.put(proceso, tiempoRestante);

                        // Verificar si terminó
                        if (tiempoRestante <= 0) {
                            proceso.setEstado(EstadoProceso.TERMINADO);
                            coresALiberar.add(coreId);
                            System.out.println("✅ t=" + tiempoActual + ": Proceso " + proceso.getId() + " terminado en Core-" + coreId);
                        }
                    }

                    // 4. Liberar cores de procesos terminados
                    for (Integer coreId : coresALiberar) {
                        procesosEnCores.remove(coreId);
                    }

                    // 5. Avanzar tiempo y esperar
                    tiempoActual++;
                    Thread.sleep(STEP_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                Platform.runLater(() -> {
                    corriendo = false;
                    System.out.println("🏁 Simulación terminada en t=" + tiempoActual);
                });
            }
        });

        simulacionThread.setDaemon(true);
        simulacionThread.start();
    }

    // Métodos simplificados - ya no necesarios con la nueva implementación
    private boolean tieneProcesosPendientes() {
        return false; // Manejado dentro de ejecutarSimulacionParalela
    }

    @FXML
    private void onStartClicked() {
        if (corriendo) {
            return;
        }

        if (procesos.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Agregue procesos antes de iniciar la simulación.");
            alert.showAndWait();
            return;
        }

        try {
            long v = Long.parseLong(txtTime.getText());
            if (v > 0) {
                STEP_DELAY_MS = v;
            }
        } catch (Exception ignored) {
            STEP_DELAY_MS = 200; // Default
        }

        corriendo = true;
        pausado = false;
        tiempoActual = 0;

        // Reiniciar sistema (simplificado)
        tiempoActual = 0;

        // Limpiar visualización
        Platform.runLater(this::prepararGantt);

        // Iniciar simulación paralela
        ejecutarSimulacionParalela();

        System.out.println("🚀 Simulación paralela iniciada con " + procesos.size() + " procesos");
    }

    @FXML
    private void onPauseClicked() {
        if (corriendo) {
            pausado = !pausado;
            System.out.println(pausado ? "⏸️ Simulación pausada" : "▶️ Simulación reanudada");
        }
    }

    @FXML
    private void onStopClicked() {
        corriendo = false;
        pausado = false;

        if (cpu != null) {
            cpu.detener();
        }

        if (manejoProcesos != null) {
            manejoProcesos.detenerEjecucion();
        }

        System.out.println("🛑 Simulación detenida");
    }

    @FXML
    private void onRetryClicked() {
        onStopClicked();

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
            scheduledExecutor = Executors.newScheduledThreadPool(2);
        }

        Platform.runLater(() -> {
            prepararGantt();
            limpiarMemoria();
        });

        System.out.println("🔄 Simulación reiniciada");
    }

    @FXML
    private void onStatsClicked() {
        if (cpu != null) {
            System.out.println(cpu.getEstadisticas());
        }
        if (manejoProcesos != null) {
            System.out.println(manejoProcesos.getEstadisticas());
        }
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
            if (proceso != null && !coloresDisponibles.isEmpty()) {
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