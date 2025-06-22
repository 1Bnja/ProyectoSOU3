/**
 * Actualiza la visualización de memoria RAM y Swapping (Disco)
 */package org.example.proyectoso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.proyectoso.models.*;
import org.example.proyectoso.planificacion.*;
import org.example.proyectoso.memoria.*;
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
    private Label lblQuantum;
    @FXML
    private Label lblTime;
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
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
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

    // Sistema de memoria
    private Memoria memoria;

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

            // Mostrar/ocultar campos según el algoritmo
            actualizarVisibilidadCampos(seleccionado);
        });

        // Inicializar visibilidad de campos (empezar con SJF)
        actualizarVisibilidadCampos("SJF");

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

        // Crear procesos predefinidos
        crearProcesosPredefinidos();

        generarGanttVacio();
        poblarMemorias();
    }

    /**
     * Crea los 5 procesos predefinidos que ocupan exactamente 2048MB de RAM
     */
    private void crearProcesosPredefinidos() {
        // Nombres predefinidos
        String[] nombres = {
                "tralalero tralala",
                "tung tung sahur",
                "bombarido crocodilo",
                "capuccion assasino",
                "br br patatim"
        };

        // Tiempos de llegada predefinidos
        int[] tiemposLlegada = {1, 2, 6, 10, 15};

        // CPU bursts fijos (entre 10 y 90)
        int[] cpuBursts = {45, 67, 23, 81, 34};

        // Tamaños de memoria fijos que suman exactamente 2048MB
        int[] tamañosMemoria = {412, 523, 367, 448, 298}; // Total: 2048MB

        // Crear los procesos
        for (int i = 0; i < 5; i++) {
            Proceso proceso = new Proceso(
                    nombres[i],
                    cpuBursts[i],
                    tamañosMemoria[i],
                    tiemposLlegada[i]
            );

            // Asignar color
            if (!coloresDisponibles.isEmpty()) {
                Color color = coloresDisponibles.remove(0);
                proceso.setColor(color);
            }

            procesos.add(proceso);
        }

        System.out.println("✨ 5 procesos predefinidos creados:");
        for (int i = 0; i < procesos.size(); i++) {
            Proceso p = procesos.get(i);
            System.out.printf("   %d. %s - Llegada: t%d, Burst: %d, RAM: %dMB%n",
                    i+1, p.getNombre(), p.getTiempoLlegada(), p.getDuracion(), p.getTamanoMemoria());
        }
        System.out.printf("   Total RAM: %dMB%n",
                procesos.stream().mapToInt(Proceso::getTamanoMemoria).sum());
    }


    private void inicializarSistemaProcesamiento() {
        // Crear CPU con 5 cores (no 6)
        cpu = new CPU(5);

        // Crear memoria de 2GB (2048 MB)
        memoria = new Memoria(2048);

        // Crear manejador de procesos
        manejoProcesos = new ManejoProcesos();

        // Configurar planificador inicial
        configurarPlanificador();

        // Crear executor para actualizaciones de interfaz
        scheduledExecutor = Executors.newScheduledThreadPool(2);

        System.out.println("🔧 Sistema de procesamiento inicializado (5 cores)");
    }

    private void configurarPlanificador() {
        String algoritmo = comboAlgoritmo.getValue();

        switch (algoritmo) {
            case "SJF":
                planificador = new SJF();
                cpu.setAlgoritmo(CPU.TipoAlgoritmo.SJF);
                System.out.println("⚙️ SJF configurado (sin quantum)");
                break;
            case "Round Robin":
                planificador = new RoundRobin(); // Necesitarás crear esta clase
                cpu.setAlgoritmo(CPU.TipoAlgoritmo.ROUND_ROBIN);
                try {
                    int quantum = Integer.parseInt(txtQuantum.getText());
                    cpu.setQuantumRoundRobin(quantum);
                    System.out.println("⚙️ Round Robin configurado con quantum: " + quantum + "ms");
                } catch (Exception e) {
                    cpu.setQuantumRoundRobin(100); // Quantum por defecto
                    System.out.println("⚙️ Round Robin configurado con quantum por defecto: 100ms");
                }
                break;
        }

        manejoProcesos.setPlanificador(planificador);
    }

    private void generarGanttVacio() {
        gridGantt.getChildren().clear();
        int tiempoTotal = 200; // Cambiado de 100 a 200

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

        // Inicializar visualización de memoria
        actualizarVisualizacionMemoria();

        System.out.println("Contenedores de memoria inicializados");
    }

    /**
     * Actualiza la visibilidad de los campos según el algoritmo seleccionado
     */
    private void actualizarVisibilidadCampos(String algoritmo) {
        boolean esRoundRobin = "Round Robin".equals(algoritmo);

        // Mostrar/ocultar campos de Quantum y Tiempo solo para Round Robin
        txtQuantum.setVisible(esRoundRobin);
        txtTime.setVisible(esRoundRobin);

        // También ocultar/mostrar las etiquetas (necesitaremos encontrarlas)
        ocultarEtiquetasCampos(!esRoundRobin);

        if (esRoundRobin) {
            // Establecer valores por defecto para Round Robin
            if (txtQuantum.getText().isEmpty()) {
                txtQuantum.setText("100");
            }
            if (txtTime.getText().isEmpty()) {
                txtTime.setText("500");
            }
            System.out.println("💡 Round Robin seleccionado - Campos Quantum y Tiempo habilitados");
        } else {
            System.out.println("💡 SJF seleccionado - Campos Quantum y Tiempo ocultados");
        }
    }

    /**
     * Oculta/muestra las etiquetas de los campos
     */
    private void ocultarEtiquetasCampos(boolean ocultar) {
        // Buscar las etiquetas en el contenedor padre
        if (txtQuantum.getParent() instanceof HBox) {
            HBox contenedor = (HBox) txtQuantum.getParent();

            contenedor.getChildren().forEach(nodo -> {
                if (nodo instanceof Label) {
                    Label etiqueta = (Label) nodo;
                    String texto = etiqueta.getText();

                    // Ocultar etiquetas "Quantum:" y "t ="
                    if ("Quantum:".equals(texto) || "t =".equals(texto)) {
                        etiqueta.setVisible(!ocultar);
                        etiqueta.setManaged(!ocultar); // Para que no ocupe espacio cuando está oculto
                    }
                }
            });

            // Ocultar/mostrar los campos también con managed para que no ocupen espacio
            txtQuantum.setManaged(!ocultar);
            txtTime.setManaged(!ocultar);
        }
    }
    private void actualizarVisualizacionMemoria() {
        Platform.runLater(() -> {
            actualizarRAM();
            actualizarSwapping();
        });
    }

    /**
     * Actualiza la visualización de la RAM
     */
    private void actualizarRAM() {
        ramContainer.getChildren().clear();

        if (memoria == null) return;

        // Obtener bloques de memoria
        List<org.example.proyectoso.memoria.BloqueMemoria> bloques = memoria.getBloques();

        double containerHeight = ramContainer.getPrefHeight();
        double containerWidth = ramContainer.getPrefWidth();
        int memoriaTotal = memoria.getTamañoTotal();

        double yOffset = 0;

        for (org.example.proyectoso.memoria.BloqueMemoria bloque : bloques) {
            // Calcular altura proporcional al tamaño del bloque
            double alturaBloque = (double) bloque.getTamaño() / memoriaTotal * containerHeight;

            // Crear rectángulo para el bloque que ocupe todo el ancho (agregando un poco más)
            Rectangle rect = new Rectangle(containerWidth + 2, alturaBloque);
            rect.setX(-1);
            rect.setY(yOffset);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(0.5); // Stroke más delgado

            if (bloque.isOcupado()) {
                // Bloque ocupado - usar color del proceso
                Color colorProceso = bloque.getProceso().getColor();
                rect.setFill(colorProceso != null ? colorProceso : Color.LIGHTBLUE);

                // Agregar etiqueta con ID del proceso
                Label etiqueta = new Label("P" + bloque.getProceso().getId());
                etiqueta.setLayoutX(containerWidth/2 - 10);
                etiqueta.setLayoutY(yOffset + alturaBloque/2 - 8);
                etiqueta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                ramContainer.getChildren().add(etiqueta);
            } else {
                // Bloque libre
                rect.setFill(Color.LIGHTGRAY);

                // Etiqueta "LIBRE" si el bloque es lo suficientemente grande
                if (alturaBloque > 20) {
                    Label etiqueta = new Label("LIBRE");
                    etiqueta.setLayoutX(containerWidth/2 - 15);
                    etiqueta.setLayoutY(yOffset + alturaBloque/2 - 8);
                    etiqueta.setStyle("-fx-font-size: 9px;");
                    ramContainer.getChildren().add(etiqueta);
                }
            }

            ramContainer.getChildren().add(rect);
            yOffset += alturaBloque;
        }

        // Mostrar estadísticas de RAM (ajustar posición)
        Label statsRAM = new Label(String.format("RAM: %dMB / %dMB (%.1f%%)",
                memoria.getMemoriaUsada(), memoria.getTamañoTotal(), memoria.getPorcentajeUso()));
        statsRAM.setLayoutX(5);
        statsRAM.setLayoutY(containerHeight - 15);
        statsRAM.setStyle("-fx-font-size: 8px; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px;");
        ramContainer.getChildren().add(statsRAM);
    }

    /**
     * Actualiza la visualización del Swapping (Disco Duro)
     */
    private void actualizarSwapping() {
        discoContainer.getChildren().clear();

        if (memoria == null) return;

        // Obtener procesos en swapping
        List<Proceso> procesosSwap = memoria.getSwapping().getProcesosEnSwapping();

        double containerHeight = discoContainer.getPrefHeight();
        double containerWidth = discoContainer.getPrefWidth();

        if (procesosSwap.isEmpty()) {
            // Disco vacío - llenar todo el contenedor (agregando un poco más)
            Rectangle rectVacio = new Rectangle(containerWidth + 2, containerHeight - 25);
            rectVacio.setX(-1);
            rectVacio.setY(0);
            rectVacio.setFill(Color.WHITESMOKE);
            rectVacio.setStroke(Color.GRAY);
            rectVacio.setStrokeWidth(0.5); // Stroke más delgado
            discoContainer.getChildren().add(rectVacio);

            Label etiquetaVacio = new Label("DISCO VACÍO");
            etiquetaVacio.setLayoutX(containerWidth/2 - 35);
            etiquetaVacio.setLayoutY(containerHeight/2);
            etiquetaVacio.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            discoContainer.getChildren().add(etiquetaVacio);
        } else {
            // Calcular memoria total en swapping
            int memoriaSwapTotal = memoria.getSwapping().getMemoriaRequerida();
            double yOffset = 0;
            double alturaDisponible = containerHeight - 25;

            for (Proceso proceso : procesosSwap) {
                // Calcular altura proporcional
                double alturaProceso = Math.max(20, (double) proceso.getTamanoMemoria() / memoriaSwapTotal * alturaDisponible);

                // Crear rectángulo para el proceso que ocupe todo el ancho (agregando un poco más)
                Rectangle rect = new Rectangle(containerWidth + 2, alturaProceso);
                rect.setX(-1);
                rect.setY(yOffset);
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(0.5); // Stroke más delgado

                // Color del proceso
                Color colorProceso = proceso.getColor();
                rect.setFill(colorProceso != null ? colorProceso.deriveColor(0, 1, 0.7, 1) : Color.LIGHTYELLOW);

                // Etiqueta con ID del proceso (centrada)
                Label etiqueta = new Label("P" + proceso.getId() + " (" + proceso.getTamanoMemoria() + "MB)");
                etiqueta.setLayoutX(containerWidth/2 - 40);
                etiqueta.setLayoutY(yOffset + alturaProceso/2 - 8);
                etiqueta.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;");

                discoContainer.getChildren().add(rect);
                discoContainer.getChildren().add(etiqueta);

                yOffset += alturaProceso;
            }
        }

        // Mostrar estadísticas de Swapping (ajustar posición)
        Label statsSwap = new Label(String.format("Swap: %d procesos (%dMB)",
                memoria.getSwapping().getCantidadProcesos(),
                memoria.getSwapping().getMemoriaRequerida()));
        statsSwap.setLayoutX(5);
        statsSwap.setLayoutY(containerHeight - 15);
        statsSwap.setStyle("-fx-font-size: 8px; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px;");
        discoContainer.getChildren().add(statsSwap);
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
                            // Intentar asignar memoria al proceso
                            if (memoria.asignarMemoria(proceso)) {
                                procesosListos.add(proceso);
                                proceso.setEstado(EstadoProceso.LISTO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó y obtuvo memoria");
                            } else {
                                // No hay memoria, enviar a swapping
                                memoria.moverASwapping(proceso);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó pero fue a SWAP");
                            }

                            // Actualizar visualización de memoria
                            actualizarVisualizacionMemoria();
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

                        for (int coreId = 0; coreId < 5 && !procesosListos.isEmpty(); coreId++) {
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

                            // Liberar memoria del proceso terminado
                            memoria.liberarMemoria(proceso);

                            System.out.println("✅ t=" + tiempoActual + ": Proceso " + proceso.getId() + " terminado en Core-" + coreId);

                            // Actualizar visualización de memoria
                            actualizarVisualizacionMemoria();
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

        // Configurar velocidad de simulación (solo para Round Robin, para SJF usar default)
        if ("Round Robin".equals(comboAlgoritmo.getValue())) {
            try {
                long v = Long.parseLong(txtTime.getText());
                if (v > 0) {
                    STEP_DELAY_MS = v;
                }
            } catch (Exception ignored) {
                STEP_DELAY_MS = 500; // Default para Round Robin
            }
        } else {
            STEP_DELAY_MS = 200; // Default para SJF (más rápido)
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
    private void onStatsClicked() {
        if (cpu != null) {
            System.out.println(cpu.getEstadisticas());
        }
        if (manejoProcesos != null) {
            System.out.println(manejoProcesos.getEstadisticas());
        }
        if (memoria != null) {
            memoria.imprimirEstado();
        }
    }

    @FXML
    private void onAddClicked() {
        // Sin límite de procesos - pueden agregarse infinitos
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
                // Asignar color solo si hay disponibles, sino usar color por defecto
                if (!coloresDisponibles.isEmpty()) {
                    Color color = coloresDisponibles.remove(0);
                    proceso.setColor(color);
                } else {
                    // Generar color aleatorio si no hay colores predefinidos disponibles
                    java.util.Random rand = new java.util.Random();
                    Color colorAleatorio = Color.color(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
                    proceso.setColor(colorAleatorio);
                }
                procesos.add(proceso);
                System.out.println("➕ Proceso agregado: " + proceso.getNombre() + " (Total: " + procesos.size() + " procesos)");
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

    @FXML
    private void onRetryClicked() {
        onStopClicked();

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
            scheduledExecutor = Executors.newScheduledThreadPool(2);
        }

        Platform.runLater(() -> {
            prepararGantt();
            // Reiniciar memoria y actualizar visualización
            if (memoria != null) {
                memoria.reiniciar();
                actualizarVisualizacionMemoria();
            }

            // Recrear procesos predefinidos si la lista está vacía
            if (procesos.isEmpty()) {
                // Restaurar colores disponibles
                coloresDisponibles.clear();
                coloresDisponibles.addAll(Arrays.asList(
                        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
                ));
                crearProcesosPredefinidos();
            }
        });

        System.out.println("🔄 Simulación reiniciada");
    }
}