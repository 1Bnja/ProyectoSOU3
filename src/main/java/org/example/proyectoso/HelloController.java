package org.example.proyectoso;
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
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.stream.Collectors;



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

    
    private ObservableList<Proceso> procesos;
    private final List<Color> coloresDisponibles = new ArrayList<>(Arrays.asList(
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
    ));

    
    @FXML private TableView<FilaEstadoProcesos> tablaColas;
    @FXML private TableColumn<FilaEstadoProcesos, String> colNuevo;
    @FXML private TableColumn<FilaEstadoProcesos, String> colListo;
    @FXML private TableColumn<FilaEstadoProcesos, String> colEspera;
    @FXML private TableColumn<FilaEstadoProcesos, String> colTerminado;



    
    private CPU cpu;
    private ManejoProcesos manejoProcesos;
    private Planificacion planificador;

    
    private Memoria memoria;

    
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean corriendo = false;
    private volatile boolean pausado = false;
    private int tiempoActual = 0;

    private Rectangle[][] celdasGantt;
    private Map<Proceso, Integer> filaMapa = new HashMap<>();
    private int nextFila = 0;

    
    private static long STEP_DELAY_MS = 200;

    private ObservableList<FilaEstadoProcesos> datosTablaEstados;
    private List<Proceso> procesosNuevo = new ArrayList<>();
    private List<Proceso> procesosListo = new ArrayList<>();
    private List<Proceso> procesosEspera = new ArrayList<>();
    private List<Proceso> procesosTerminado = new ArrayList<>();

    
    private long tiempoInicioSimulacion;
    private String algoritmoUtilizado;
    private int quantumUtilizado;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        
        comboAlgoritmo.getItems().addAll("SJF", "Round Robin");
        comboAlgoritmo.setValue("SJF");

        comboAlgoritmo.setOnAction(event -> {
            String seleccionado = comboAlgoritmo.getValue();
            System.out.println("Algoritmo seleccionado: " + seleccionado);

            
            actualizarVisibilidadCampos(seleccionado);
        });

        
        actualizarVisibilidadCampos("SJF");

        
        colProceso.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombre()));
        colLlegada.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTiempoLlegada()).asObject());
        colBurst.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getDuracion()).asObject());
        colMemoria.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTamanoMemoria()).asObject());

        
        procesos = FXCollections.observableArrayList();
        tablaProcesos.setItems(procesos);

        
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

        
        inicializarSistemaProcesamiento();

        
        crearProcesosPredefinidos();

        generarGanttVacio();
        poblarMemorias();
        poblarMemorias();
        inicializarTablaColas(); 

    }

    
    private void crearProcesosPredefinidos() {
        
        String[] nombres = {
                "tralalero tralala",
                "tung tung sahur",
                "bombarido crocodilo",
                "capuccion assasino",
                "br br patatim"
        };

        
        int[] tiemposLlegada = {1, 2, 6, 10, 15};

        
        int[] cpuBursts = {45, 67, 23, 81, 34};

        
        int[] tamañosMemoria = {412, 523, 367, 448, 298}; 

        
        for (int i = 0; i < 5; i++) {
            Proceso proceso = new Proceso(
                    nombres[i],
                    cpuBursts[i],
                    tamañosMemoria[i],
                    tiemposLlegada[i]
            );

            
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
        
        cpu = new CPU(6);

        
        memoria = new Memoria(2048);

        
        manejoProcesos = new ManejoProcesos();

        
        configurarPlanificador();

        
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
                try {
                    int quantum = Integer.parseInt(txtQuantum.getText());
                    cpu.setQuantumRoundRobin(quantum);
                    System.out.println("⚙️ Round Robin configurado con quantum: " + quantum + "ms");
                } catch (Exception e) {
                    cpu.setQuantumRoundRobin(100); 
                    System.out.println("⚙️ Round Robin configurado con quantum por defecto: 100ms");
                }
                break;
        }

        manejoProcesos.setPlanificador(planificador);
    }

    private void generarGanttVacio() {
        gridGantt.getChildren().clear();
        int tiempoTotal = 1000; 

        celdasGantt = new Rectangle[6][tiempoTotal]; 

        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t, 0);
        }

        for (int core = 0; core < 6; core++) {
            
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

        
        actualizarVisualizacionMemoria();

        System.out.println("Contenedores de memoria inicializados");
    }

    
    private void actualizarVisibilidadCampos(String algoritmo) {
        boolean esRoundRobin = "Round Robin".equals(algoritmo);

        
        txtQuantum.setVisible(esRoundRobin);
        txtTime.setVisible(esRoundRobin);

        
        ocultarEtiquetasCampos(!esRoundRobin);

        if (esRoundRobin) {
            
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

    
    private void ocultarEtiquetasCampos(boolean ocultar) {
        
        if (txtQuantum.getParent() instanceof HBox) {
            HBox contenedor = (HBox) txtQuantum.getParent();

            contenedor.getChildren().forEach(nodo -> {
                if (nodo instanceof Label) {
                    Label etiqueta = (Label) nodo;
                    String texto = etiqueta.getText();

                    
                    if ("Quantum:".equals(texto) || "t =".equals(texto)) {
                        etiqueta.setVisible(!ocultar);
                        etiqueta.setManaged(!ocultar); 
                    }
                }
            });

            
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

    
    private void actualizarRAM() {
        ramContainer.getChildren().clear();

        if (memoria == null) return;

        
        List<org.example.proyectoso.memoria.BloqueMemoria> bloques = memoria.getBloques();

        double containerHeight = ramContainer.getPrefHeight();
        double containerWidth = ramContainer.getPrefWidth();
        int memoriaTotal = memoria.getTamañoTotal();

        double yOffset = 0;

        for (org.example.proyectoso.memoria.BloqueMemoria bloque : bloques) {
            
            double alturaBloque = (double) bloque.getTamaño() / memoriaTotal * containerHeight;

            
            Rectangle rect = new Rectangle(containerWidth + 2, alturaBloque);
            rect.setX(-1);
            rect.setY(yOffset);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(0.5); 

            if (bloque.isOcupado()) {
                
                Color colorProceso = bloque.getProceso().getColor();
                rect.setFill(colorProceso != null ? colorProceso : Color.LIGHTBLUE);

                
                Label etiqueta = new Label("P" + bloque.getProceso().getId());
                etiqueta.setLayoutX(containerWidth/2 - 10);
                etiqueta.setLayoutY(yOffset + alturaBloque/2 - 8);
                etiqueta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                ramContainer.getChildren().add(etiqueta);
            } else {
                
                rect.setFill(Color.LIGHTGRAY);

                
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

        
        Label statsRAM = new Label(String.format("RAM: %dMB / %dMB (%.1f%%)",
                memoria.getMemoriaUsada(), memoria.getTamañoTotal(), memoria.getPorcentajeUso()));
        statsRAM.setLayoutX(5);
        statsRAM.setLayoutY(containerHeight - 15);
        statsRAM.setStyle("-fx-font-size: 8px; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px;");
        ramContainer.getChildren().add(statsRAM);
    }

    
    private void actualizarSwapping() {
        discoContainer.getChildren().clear();

        if (memoria == null) return;

        
        List<Proceso> procesosSwap = memoria.getSwapping().getProcesosEnSwapping();

        double containerHeight = discoContainer.getPrefHeight();
        double containerWidth = discoContainer.getPrefWidth();

        if (procesosSwap.isEmpty()) {
            
            Rectangle rectVacio = new Rectangle(containerWidth + 2, containerHeight - 25);
            rectVacio.setX(-1);
            rectVacio.setY(0);
            rectVacio.setFill(Color.WHITESMOKE);
            rectVacio.setStroke(Color.GRAY);
            rectVacio.setStrokeWidth(0.5); 
            discoContainer.getChildren().add(rectVacio);

            Label etiquetaVacio = new Label("DISCO VACÍO");
            etiquetaVacio.setLayoutX(containerWidth/2 - 35);
            etiquetaVacio.setLayoutY(containerHeight/2);
            etiquetaVacio.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            discoContainer.getChildren().add(etiquetaVacio);
        } else {
            
            int memoriaSwapTotal = memoria.getSwapping().getMemoriaRequerida();
            double yOffset = 0;
            double alturaDisponible = containerHeight - 25;

            for (Proceso proceso : procesosSwap) {
                
                double alturaProceso = Math.max(20, (double) proceso.getTamanoMemoria() / memoriaSwapTotal * alturaDisponible);

                
                Rectangle rect = new Rectangle(containerWidth + 2, alturaProceso);
                rect.setX(-1);
                rect.setY(yOffset);
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(0.5); 

                
                Color colorProceso = proceso.getColor();
                rect.setFill(colorProceso != null ? colorProceso.deriveColor(0, 1, 0.7, 1) : Color.LIGHTYELLOW);

                
                Label etiqueta = new Label("P" + proceso.getId() + " (" + proceso.getTamanoMemoria() + "MB)");
                etiqueta.setLayoutX(containerWidth/2 - 40);
                etiqueta.setLayoutY(yOffset + alturaProceso/2 - 8);
                etiqueta.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;");

                discoContainer.getChildren().add(rect);
                discoContainer.getChildren().add(etiqueta);

                yOffset += alturaProceso;
            }
        }

        
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

    
    private void ejecutarSimulacionParalela() {
        List<Proceso> listaProcesos = new ArrayList<>(procesos);

        
        
        for (Proceso p : listaProcesos) {
            p.reiniciarTiempos();
        }
        

        
        filaMapa.clear();
        nextFila = 0;
        for (Proceso p : listaProcesos) {
            if (!filaMapa.containsKey(p)) {
                filaMapa.put(p, nextFila++);
            }
        }

        
        List<Proceso> procesosPendientes = new ArrayList<>(listaProcesos);
        List<Proceso> procesosListos = new ArrayList<>();
        Map<Integer, Proceso> procesosEnCores = new HashMap<>();
        Map<Proceso, Integer> tiempoRestanteProceso = new HashMap<>();

        
        for (Proceso p : listaProcesos) {
            actualizarEstadoProceso(p, EstadoProceso.NUEVO);
            tiempoRestanteProceso.put(p, p.getDuracion());
        }

        
        actualizarTablaColas();

        Platform.runLater(this::prepararGantt);

        Thread simulacionThread = new Thread(() -> {
            try {
                tiempoActual = 0;

                while (corriendo && (!procesosPendientes.isEmpty() || !procesosListos.isEmpty() || !procesosEnCores.isEmpty())) {

                    while (pausado && corriendo) {
                        Thread.sleep(100);
                    }

                    if (!corriendo) break;

                    
                    procesosPendientes.removeIf(proceso -> {
                        if (proceso.getTiempoLlegada() <= tiempoActual) {
                            if (memoria.asignarMemoria(proceso)) {
                                procesosListos.add(proceso);
                                actualizarEstadoProceso(proceso, EstadoProceso.LISTO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó y obtuvo memoria");
                            } else {
                                memoria.moverASwapping(proceso);
                                actualizarEstadoProceso(proceso, EstadoProceso.ESPERANDO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó pero fue a SWAP");
                            }

                            actualizarVisualizacionMemoria();
                            return true;
                        }
                        return false;
                    });

                    
                    if (!procesosListos.isEmpty()) {
                        procesosListos.sort((p1, p2) -> Integer.compare(
                                tiempoRestanteProceso.get(p1),
                                tiempoRestanteProceso.get(p2)
                        ));

                        for (int coreId = 0; coreId < 6 && !procesosListos.isEmpty(); coreId++) {
                            if (!procesosEnCores.containsKey(coreId)) {
                                Proceso proceso = procesosListos.remove(0);
                                procesosEnCores.put(coreId, proceso);
                                actualizarEstadoProceso(proceso, EstadoProceso.EJECUTANDO);

                                
                                proceso.marcarInicioEjecucion(tiempoActual);
                                

                                System.out.println("🔧 t=" + tiempoActual + ": Core-" + coreId + " ejecuta Proceso " + proceso.getId());
                            }
                        }
                    }

                    
                    List<Integer> coresALiberar = new ArrayList<>();
                    for (Map.Entry<Integer, Proceso> entry : procesosEnCores.entrySet()) {
                        int coreId = entry.getKey();
                        Proceso proceso = entry.getValue();

                        
                        final int finalTiempo = tiempoActual;
                        final int finalCore = coreId;
                        Platform.runLater(() -> {
                            Color color = proceso.getColor() != null ? proceso.getColor() : Color.LIGHTBLUE;
                            pintarCeldaCore(finalCore, finalTiempo, color);
                        });

                        
                        int tiempoRestante = tiempoRestanteProceso.get(proceso) - 1;
                        tiempoRestanteProceso.put(proceso, tiempoRestante);

                        
                        if (tiempoRestante <= 0) {
                            actualizarEstadoProceso(proceso, EstadoProceso.TERMINADO);

                            
                            proceso.marcarFinalizacion(tiempoActual + 1);
                            

                            coresALiberar.add(coreId);

                            
                            memoria.liberarMemoria(proceso);

                            System.out.println("✅ t=" + tiempoActual + ": Proceso " + proceso.getId() + " terminado en Core-" + coreId);

                            actualizarVisualizacionMemoria();
                        }
                    }

                    
                    for (Integer coreId : coresALiberar) {
                        procesosEnCores.remove(coreId);
                    }

                    
                    actualizarTablaColas();

                    
                    tiempoActual++;
                    Thread.sleep(STEP_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                Platform.runLater(() -> {
                    corriendo = false;
                    actualizarTablaColas();
                    System.out.println("🏁 Simulación terminada en t=" + tiempoActual);
                    generarArchivoEstadisticas();
                });
            }
        });

        simulacionThread.setDaemon(true);
        simulacionThread.start();
    }

    @FXML
    private void onStartClicked() {
        if (cpu != null) {
            System.out.println(cpu.getEstadisticas());
        }
        if (manejoProcesos != null) {
            System.out.println(manejoProcesos.getEstadisticas());
        }
        if (memoria != null) {
            memoria.imprimirEstado();
        }
        if (corriendo) {
            return;
        }

        if (procesos.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Agregue procesos antes de iniciar la simulación.");
            alert.showAndWait();
            return;
        }
        tiempoInicioSimulacion = System.currentTimeMillis();
        algoritmoUtilizado = comboAlgoritmo.getValue();
        quantumUtilizado = "Round Robin".equals(algoritmoUtilizado) ?
                Integer.parseInt(txtQuantum.getText().isEmpty() ? "100" : txtQuantum.getText()) : 0;


        
        if ("Round Robin".equals(comboAlgoritmo.getValue())) {
            try {
                long v = Long.parseLong(txtTime.getText());
                if (v > 0) {
                    STEP_DELAY_MS = v;
                }
            } catch (Exception ignored) {
                STEP_DELAY_MS = 500; 
            }
        } else {
            STEP_DELAY_MS = 200; 
        }

        corriendo = true;
        pausado = false;
        tiempoActual = 0;

        
        tiempoActual = 0;

        
        Platform.runLater(this::prepararGantt);

        
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
                
                if (!coloresDisponibles.isEmpty()) {
                    Color color = coloresDisponibles.remove(0);
                    proceso.setColor(color);
                } else {
                    
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
            limpiarTablaColas(); 

            if (memoria != null) {
                memoria.reiniciar();
                actualizarVisualizacionMemoria();
            }

            if (procesos.isEmpty()) {
                coloresDisponibles.clear();
                coloresDisponibles.addAll(Arrays.asList(
                        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
                ));
                crearProcesosPredefinidos();
            }

            
            actualizarTablaColas(); 
        });

        System.out.println("🔄 Simulación reiniciada");
    }

    private void inicializarTablaColas() {
        
        datosTablaEstados = FXCollections.observableArrayList();

        
        colNuevo.setCellValueFactory(new PropertyValueFactory<>("nuevo"));
        colListo.setCellValueFactory(new PropertyValueFactory<>("listo"));
        colEspera.setCellValueFactory(new PropertyValueFactory<>("espera"));
        colTerminado.setCellValueFactory(new PropertyValueFactory<>("terminado"));

        
        tablaColas.setItems(datosTablaEstados);

        
        actualizarTablaColas();

        System.out.println("📋 Tabla de colas inicializada");
    }
    private void actualizarTablaColas() {
        Platform.runLater(() -> {
            
            procesosNuevo.clear();
            procesosListo.clear();
            procesosEspera.clear();
            procesosTerminado.clear();

            
            for (Proceso proceso : procesos) {
                switch (proceso.getEstado()) {
                    case NUEVO:
                        procesosNuevo.add(proceso);
                        break;
                    case LISTO:
                        procesosListo.add(proceso);
                        break;
                    case ESPERANDO:
                        procesosEspera.add(proceso);
                        break;
                    case TERMINADO:
                        procesosTerminado.add(proceso);
                        break;
                    case EJECUTANDO:
                        
                        procesosListo.add(proceso);
                        break;
                }
            }

            
            datosTablaEstados.clear();

            
            int maxFilas = Math.max(Math.max(procesosNuevo.size(), procesosListo.size()),
                    Math.max(procesosEspera.size(), procesosTerminado.size()));

            
            if (maxFilas == 0) {
                maxFilas = 1;
            }

            
            for (int i = 0; i < maxFilas; i++) {
                String nuevo = i < procesosNuevo.size() ?
                        formatearProceso(procesosNuevo.get(i)) : "";
                String listo = i < procesosListo.size() ?
                        formatearProceso(procesosListo.get(i)) : "";
                String espera = i < procesosEspera.size() ?
                        formatearProceso(procesosEspera.get(i)) : "";
                String terminado = i < procesosTerminado.size() ?
                        formatearProceso(procesosTerminado.get(i)) : "";

                datosTablaEstados.add(new FilaEstadoProcesos(nuevo, listo, espera, terminado));
            }
        });
    }

    private String formatearProceso(Proceso proceso) {
        if (proceso == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(proceso.getId());

        
        switch (proceso.getEstado()) {
            case NUEVO:
                sb.append(" (").append(proceso.getTamanoMemoria()).append("MB)");
                break;
            case LISTO:
                sb.append(" (").append(proceso.getTiempoRestante()).append("ms)");
                break;
            case EJECUTANDO:
                sb.append(" [EXEC] (").append(proceso.getTiempoRestante()).append("ms)");
                break;
            case ESPERANDO:
                sb.append(" (I/O)");
                break;
            case TERMINADO:
                sb.append(" ✓");
                break;
        }

        return sb.toString();
    }

    
    private void actualizarEstadoProceso(Proceso proceso, EstadoProceso nuevoEstado) {
        if (proceso != null && proceso.getEstado() != nuevoEstado) {
            EstadoProceso estadoAnterior = proceso.getEstado();
            proceso.setEstado(nuevoEstado);

            System.out.println("🔄 Proceso " + proceso.getId() +
                    " cambió de " + estadoAnterior + " a " + nuevoEstado);

            
            actualizarTablaColas();
        }
    }
    private void limpiarTablaColas() {
        Platform.runLater(() -> {
            datosTablaEstados.clear();
            procesosNuevo.clear();
            procesosListo.clear();
            procesosEspera.clear();
            procesosTerminado.clear();

            
            datosTablaEstados.add(new FilaEstadoProcesos("", "", "", ""));
        });
    }
    public String getEstadisticasEstados() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ESTADÍSTICAS DE ESTADOS ===\n");
        stats.append("Procesos Nuevos: ").append(procesosNuevo.size()).append("\n");
        stats.append("Procesos Listos: ").append(procesosListo.size()).append("\n");
        stats.append("Procesos en Espera: ").append(procesosEspera.size()).append("\n");
        stats.append("Procesos Terminados: ").append(procesosTerminado.size()).append("\n");
        stats.append("Total de Procesos: ").append(procesos.size()).append("\n");

        if (!procesos.isEmpty()) {
            double porcentajeTerminados = (double) procesosTerminado.size() / procesos.size() * 100;
            stats.append("Progreso: ").append(String.format("%.1f%%", porcentajeTerminados)).append("\n");
        }

        return stats.toString();
    }
    private void generarArchivoEstadisticas() {
        try {
            
            LocalDateTime ahora = LocalDateTime.now();
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String nombreArchivo = "estadisticas_simulacion_" + formato.format(ahora) + ".txt";

            
            FileWriter writer = new FileWriter(nombreArchivo);

            
            writer.write("=" .repeat(80) + "\n");
            writer.write("           REPORTE DE ESTADÍSTICAS DE SIMULACIÓN\n");
            writer.write("=" .repeat(80) + "\n\n");

            
            escribirInformacionGeneral(writer);

            
            escribirEstadisticasPorProceso(writer);

            
            escribirEstadisticasRendimiento(writer);

            
            escribirEstadisticasMemoria(writer);

            
            escribirEstadisticasCPU(writer);

            
            escribirResumenFinal(writer);

            writer.close();

            System.out.println("📄 Archivo de estadísticas generado: " + nombreArchivo);

        } catch (IOException e) {
            System.err.println("❌ Error al generar archivo de estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void escribirInformacionGeneral(FileWriter writer) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatoCompleto = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        writer.write("INFORMACIÓN GENERAL\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Fecha y hora: " + formatoCompleto.format(ahora) + "\n");
        writer.write("Algoritmo utilizado: " + algoritmoUtilizado + "\n");

        if ("Round Robin".equals(algoritmoUtilizado)) {
            writer.write("Quantum: " + quantumUtilizado + " ms\n");
        }

        writer.write("Tiempo total de simulación: " + tiempoActual + " unidades\n");
        writer.write("Duración real: " + calcularDuracionReal() + "\n");
        writer.write("Total de procesos: " + procesos.size() + "\n");
        writer.write("Cores utilizados: 5\n");

        if (memoria != null) {
            writer.write("Memoria total: " + memoria.getTamañoTotal() + " MB\n");
        }

        writer.write("\n");
    }
    private void escribirEstadisticasPorProceso(FileWriter writer) throws IOException {
        writer.write("ESTADÍSTICAS POR PROCESO\n");
        writer.write("-".repeat(50) + "\n");
        writer.write(String.format("%-4s %-20s %-8s %-8s %-8s %-10s %-10s %-10s %-10s %-8s\n",
                "ID", "Nombre", "Llegada", "Burst", "Memoria", "T.Inicio", "T.Fin", "T.Espera", "T.Retorno", "Estado"));
        writer.write("-".repeat(110) + "\n");

        for (Proceso proceso : procesos) {
            writer.write(String.format("%-4d %-20s %-8d %-8d %-8d %-10d %-10d %-10d %-10d %-8s\n",
                    proceso.getId(),
                    truncarTexto(proceso.getNombre(), 20),
                    proceso.getTiempoLlegada(),
                    proceso.getDuracion(),
                    proceso.getTamanoMemoria(),
                    proceso.getTiempoInicioReal(), 
                    proceso.getTiempoFinalizacionReal(), 
                    proceso.getTiempoEspera(),
                    proceso.getTiempoRetorno(),
                    proceso.getEstado().toString()));
        }

        
        writer.write("\nVERIFICACIÓN DEL ALGORITMO SJF\n");
        writer.write("-".repeat(50) + "\n");
        verificarSJF(writer);
        writer.write("\n");
    }

    private void verificarSJF(FileWriter writer) throws IOException {
        
        Map<Integer, List<Proceso>> procesosPorLlegada = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .collect(Collectors.groupingBy(Proceso::getTiempoLlegada));

        writer.write("Análisis del orden de ejecución por SJF:\n\n");

        for (Map.Entry<Integer, List<Proceso>> entry : procesosPorLlegada.entrySet()) {
            int tiempoLlegada = entry.getKey();
            List<Proceso> procesosGrupo = entry.getValue();

            if (procesosGrupo.size() > 1) {
                writer.write("Procesos que llegaron en t=" + tiempoLlegada + ":\n");

                
                List<Proceso> ordenSJF = procesosGrupo.stream()
                        .sorted(Comparator.comparingInt(Proceso::getDuracion))
                        .collect(Collectors.toList());

                
                List<Proceso> ordenReal = procesosGrupo.stream()
                        .sorted(Comparator.comparingInt(Proceso::getTiempoInicioReal))
                        .collect(Collectors.toList());

                writer.write("  Orden esperado (SJF): ");
                for (int i = 0; i < ordenSJF.size(); i++) {
                    writer.write("P" + ordenSJF.get(i).getId() + "(burst=" + ordenSJF.get(i).getDuracion() + ")");
                    if (i < ordenSJF.size() - 1) writer.write(" → ");
                }
                writer.write("\n");

                writer.write("  Orden real ejecutado: ");
                for (int i = 0; i < ordenReal.size(); i++) {
                    writer.write("P" + ordenReal.get(i).getId() + "(inicio=" + ordenReal.get(i).getTiempoInicioReal() + ")");
                    if (i < ordenReal.size() - 1) writer.write(" → ");
                }
                writer.write("\n");

                
                boolean sjfCorrecto = true;
                for (int i = 0; i < Math.min(ordenSJF.size(), ordenReal.size()); i++) {
                    if (ordenSJF.get(i).getId() != ordenReal.get(i).getId()) {
                        sjfCorrecto = false;
                        break;
                    }
                }

                writer.write("  ✓ SJF implementado correctamente: " + (sjfCorrecto ? "SÍ" : "NO") + "\n\n");
            }
        }

        
        writer.write("Procesos que llegaron individualmente:\n");
        for (Proceso proceso : procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .sorted(Comparator.comparingInt(Proceso::getTiempoLlegada))
                .collect(Collectors.toList())) {

            long procesosEnMismoTiempo = procesos.stream()
                    .filter(p -> p.getTiempoLlegada() == proceso.getTiempoLlegada())
                    .count();

            if (procesosEnMismoTiempo == 1) {
                writer.write("  P" + proceso.getId() + " (llegada=" + proceso.getTiempoLlegada() +
                        ", burst=" + proceso.getDuracion() +
                        ", inicio=" + proceso.getTiempoInicioReal() +
                        ", espera=" + proceso.getTiempoEspera() + ")\n");
            }
        }
    }
    private void escribirEstadisticasRendimiento(FileWriter writer) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");

        
        double promedioEspera = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoEspera)
                .average()
                .orElse(0.0);

        double promedioRespuesta = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoRespuesta)
                .average()
                .orElse(0.0);

        double promedioRetorno = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoRetorno)
                .average()
                .orElse(0.0);

        int procesosTerminados = (int) procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .count();

        double throughput = procesosTerminados > 0 ? (double) procesosTerminados / tiempoActual : 0.0;

        writer.write("ESTADÍSTICAS DE RENDIMIENTO\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Procesos completados: " + procesosTerminados + " / " + procesos.size() + "\n");
        writer.write("Porcentaje completado: " + df.format((double) procesosTerminados / procesos.size() * 100) + "%\n");
        writer.write("Tiempo promedio de espera: " + df.format(promedioEspera) + " unidades\n");
        writer.write("Tiempo promedio de respuesta: " + df.format(promedioRespuesta) + " unidades\n");
        writer.write("Tiempo promedio de retorno: " + df.format(promedioRetorno) + " unidades\n");
        writer.write("Throughput: " + df.format(throughput) + " procesos/unidad tiempo\n");
        writer.write("Eficiencia del sistema: " + df.format(calcularEficiencia()) + "%\n");
        writer.write("\n");
    }
    private void escribirEstadisticasMemoria(FileWriter writer) throws IOException {
        if (memoria == null) {
            writer.write("ESTADÍSTICAS DE MEMORIA\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("Sistema de memoria no disponible\n\n");
            return;
        }

        DecimalFormat df = new DecimalFormat("#.##");

        writer.write("ESTADÍSTICAS DE MEMORIA\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Memoria total: " + memoria.getTamañoTotal() + " MB\n");
        writer.write("Memoria usada: " + memoria.getMemoriaUsada() + " MB\n");
        writer.write("Memoria libre: " + memoria.getMemoriaLibre() + " MB\n");
        writer.write("Porcentaje de uso: " + df.format(memoria.getPorcentajeUso()) + "%\n");

        if (memoria.getSwapping() != null) {
            writer.write("Procesos en swap: " + memoria.getSwapping().getCantidadProcesos() + "\n");
            writer.write("Memoria en swap: " + memoria.getSwapping().getMemoriaRequerida() + " MB\n");
        }

        
        int bloquesLibres = (int) memoria.getBloques().stream()
                .filter(bloque -> !bloque.isOcupado())
                .count();
        writer.write("Bloques libres: " + bloquesLibres + "\n");
        writer.write("Fragmentación: " + (bloquesLibres > 1 ? "SÍ" : "NO") + "\n");
        writer.write("\n");
    }

    private void escribirEstadisticasCPU(FileWriter writer) throws IOException {
        if (cpu == null) {
            writer.write("ESTADÍSTICAS DE CPU\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("Sistema de CPU no disponible\n\n");
            return;
        }

        DecimalFormat df = new DecimalFormat("#.##");

        writer.write("ESTADÍSTICAS DE CPU\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Número de cores: " + cpu.getNumeroCores() + "\n");
        writer.write("Cores libres: " + cpu.getCoresLibresCount() + "\n");
        writer.write("Cores ocupados: " + cpu.getCoresOcupadosCount() + "\n");
        writer.write("Uso promedio de CPU: " + df.format(cpu.getUsoPromedioCpu()) + "%\n");
        writer.write("Procesos ejecutados: " + cpu.getProcesosTotalesEjecutados() + "\n");

        
        writer.write("\nEstado por core:\n");
        for (int i = 0; i < cpu.getNumeroCores(); i++) {
            Core core = cpu.getCore(i);
            if (core != null) {
                writer.write("  Core " + i + ": " +
                        (core.isLibre() ? "LIBRE" : "OCUPADO") +
                        " - Uso: " + df.format(core.getPorcentajeUso()) + "%\n");
            }
        }
        writer.write("\n");
    }

    private void escribirResumenFinal(FileWriter writer) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");

        writer.write("RESUMEN FINAL Y ANÁLISIS\n");
        writer.write("-".repeat(50) + "\n");

        
        Proceso procesoMasRapido = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .min((p1, p2) -> Integer.compare(p1.getTiempoRetorno(), p2.getTiempoRetorno()))
                .orElse(null);

        Proceso procesoMasLento = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .max((p1, p2) -> Integer.compare(p1.getTiempoRetorno(), p2.getTiempoRetorno()))
                .orElse(null);

        if (procesoMasRapido != null) {
            writer.write("Proceso más rápido: P" + procesoMasRapido.getId() +
                    " (" + procesoMasRapido.getNombre() + ") - " +
                    procesoMasRapido.getTiempoRetorno() + " unidades\n");
        }

        if (procesoMasLento != null) {
            writer.write("Proceso más lento: P" + procesoMasLento.getId() +
                    " (" + procesoMasLento.getNombre() + ") - " +
                    procesoMasLento.getTiempoRetorno() + " unidades\n");
        }

        
        writer.write("\nANÁLISIS DEL ALGORITMO SJF:\n");

        
        double tiempoEsperaPromedio = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoEspera)
                .average()
                .orElse(0.0);

        writer.write("Tiempo de espera promedio: " + df.format(tiempoEsperaPromedio) + " unidades\n");

        
        long procesosConEspera = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .filter(p -> p.getTiempoEspera() > 0)
                .count();

        writer.write("Procesos que tuvieron que esperar: " + procesosConEspera + "\n");
        writer.write("Eficiencia del algoritmo " + algoritmoUtilizado + ": " +
                df.format(calcularEficiencia()) + "%\n");

        
        writer.write("\nRECOMENDACIONES ESPECÍFICAS PARA SJF:\n");
        if (tiempoEsperaPromedio < 10) {
            writer.write("✓ SJF está funcionando eficientemente - bajo tiempo de espera promedio.\n");
        }
        if (procesosConEspera > 0) {
            writer.write("- " + procesosConEspera + " procesos experimentaron espera debido a llegadas tardías.\n");
        }

        generarRecomendaciones(writer);

        writer.write("\n" + "=".repeat(80) + "\n");
        writer.write("Fin del reporte - Generado automáticamente por el Simulador de SO\n");
        writer.write("=".repeat(80) + "\n");
    }

    private void generarRecomendaciones(FileWriter writer) throws IOException {
        double promedioEspera = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoEspera)
                .average()
                .orElse(0.0);

        if (promedioEspera > 50) {
            writer.write("- El tiempo de espera promedio es alto. Considere optimizar el algoritmo de planificación.\n");
        }

        if (memoria != null && memoria.getPorcentajeUso() > 90) {
            writer.write("- Alto uso de memoria detectado. Considere aumentar la RAM del sistema.\n");
        }

        if (cpu != null && cpu.getUsoPromedioCpu() < 50) {
            writer.write("- Bajo uso de CPU. El sistema tiene capacidad para más procesos.\n");
        }

        int procesosNoCompletados = (int) procesos.stream()
                .filter(p -> p.getEstado() != EstadoProceso.TERMINADO)
                .count();

        if (procesosNoCompletados > 0) {
            writer.write("- " + procesosNoCompletados + " proceso(s) no completado(s). Considere aumentar el tiempo de simulación.\n");
        }

        if ("SJF".equals(algoritmoUtilizado)) {
            writer.write("- SJF es eficiente para minimizar tiempo de espera promedio.\n");
        } else if ("Round Robin".equals(algoritmoUtilizado)) {
            writer.write("- Round Robin proporciona buena respuesta interactiva.\n");
            if (quantumUtilizado < 50) {
                writer.write("- Quantum bajo puede causar muchos cambios de contexto.\n");
            }
        }
    }
    private String calcularDuracionReal() {
        if (tiempoInicioSimulacion > 0) {
            long duracionMs = System.currentTimeMillis() - tiempoInicioSimulacion;
            return String.format("%.2f segundos", duracionMs / 1000.0);
        }
        return "No disponible";
    }

    private double calcularEficiencia() {
        if (procesos.isEmpty() || tiempoActual == 0) {
            return 0.0;
        }

        int procesosTerminados = (int) procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .count();

        int tiempoTotalCPU = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getDuracion)
                .sum();

        int tiempoTotalDisponible = tiempoActual * 5; 

        return tiempoTotalDisponible > 0 ?
                (double) tiempoTotalCPU / tiempoTotalDisponible * 100 : 0.0;
    }

    private String truncarTexto(String texto, int longitud) {
        if (texto == null) return "";
        return texto.length() > longitud ?
                texto.substring(0, longitud - 3) + "..." : texto;
    }




}