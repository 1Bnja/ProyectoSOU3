<<<<<<< HEAD
package org.example.proyectoso;
=======
/**
 * Actualiza la visualización de memoria RAM y Swapping (Disco)
 * CORREGIDO: Ahora usa 6 cores correctamente
 */
package org.example.proyectoso;

>>>>>>> origin/Benja
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
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.BROWN
    ));

    // Tabla de colas de procesos
    @FXML private TableView<FilaEstadoProcesos> tablaColas;
    @FXML private TableColumn<FilaEstadoProcesos, String> colNuevo;
    @FXML private TableColumn<FilaEstadoProcesos, String> colListo;
    @FXML private TableColumn<FilaEstadoProcesos, String> colEspera;
    @FXML private TableColumn<FilaEstadoProcesos, String> colTerminado;

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

<<<<<<< HEAD
    private ObservableList<FilaEstadoProcesos> datosTablaEstados;
    private List<Proceso> procesosNuevo = new ArrayList<>();
    private List<Proceso> procesosListo = new ArrayList<>();
    private List<Proceso> procesosEspera = new ArrayList<>();
    private List<Proceso> procesosTerminado = new ArrayList<>();
=======
    // CONSTANTE PARA 6 CORES
    private static final int NUMERO_CORES = 6;
>>>>>>> origin/Benja

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
        poblarMemorias();
        inicializarTablaColas(); // AGREGAR ESTA LÍNEA

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
        int[] cpuBursts = {250, 180, 320, 150, 200};

        // Tamaños de memoria fijos que suman exactamente 2048MB
        int[] tamañosMemoria = {200, 200, 200, 500, 500}; // Total: 2048MB

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
        // CORREGIDO: Crear CPU con 6 cores (no 5)
        cpu = new CPU(NUMERO_CORES);

        // Crear memoria de 2GB (2048 MB)
        memoria = new Memoria(2048);

        // Crear manejador de procesos
        manejoProcesos = new ManejoProcesos();

        // Configurar planificador inicial
        configurarPlanificador();

        // Crear executor para actualizaciones de interfaz
        scheduledExecutor = Executors.newScheduledThreadPool(2);

        System.out.println("🔧 Sistema de procesamiento inicializado (" + NUMERO_CORES + " cores)");
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
                    cpu.setQuantumRoundRobin(100); // Quantum por defecto
                    System.out.println("⚙️ Round Robin configurado con quantum por defecto: 100ms");
                }
                break;
        }

        manejoProcesos.setPlanificador(planificador);
    }

    private void generarGanttVacio() {
        gridGantt.getChildren().clear();
        int tiempoTotal = 1000; // Tiempo máximo para mostrar

        // CORREGIDO: Crear matriz para 6 cores
        celdasGantt = new Rectangle[NUMERO_CORES][tiempoTotal];

        // Agregar etiquetas de tiempo en la primera fila
        for (int t = 0; t < tiempoTotal; t++) {
            Label tiempoLabel = new Label("t" + t);
            tiempoLabel.setPrefSize(30, 30);
            tiempoLabel.setStyle("-fx-border-color: gray; -fx-alignment: center;");
            gridGantt.add(tiempoLabel, t + 1, 0); // +1 para dejar espacio para etiquetas de cores
        }

        // CORREGIDO: Crear filas para 6 cores
        for (int core = 0; core < NUMERO_CORES; core++) {
            // Agregar etiqueta de core en la primera columna
            Label coreLabel = new Label("C" + core);
            coreLabel.setPrefSize(30, 30);
            coreLabel.setStyle("-fx-border-color: gray; -fx-alignment: center; -fx-background-color: lightgray;");
            gridGantt.add(coreLabel, 0, core + 1);

            // Crear celdas para este core
            for (int col = 0; col < tiempoTotal; col++) {
                Rectangle bloque = new Rectangle(30, 30);
                bloque.setStroke(Color.GRAY);
                bloque.setFill(Color.TRANSPARENT);
                gridGantt.add(bloque, col + 1, core + 1); // +1 para dejar espacio para etiqueta de core
                celdasGantt[core][col] = bloque;
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
     * Método helper para obtener el valor del quantum desde el TextField
     * @return valor del quantum, o 100 por defecto si hay error
     */
    private int obtenerQuantumValue() {
        try {
            String textoQuantum = txtQuantum.getText();
            if (textoQuantum == null || textoQuantum.trim().isEmpty()) {
                return 100; // Default si está vacío
            }
            int valor = Integer.parseInt(textoQuantum.trim());
            return valor > 0 ? valor : 100; // Asegurar que sea positivo
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Error parseando quantum, usando valor por defecto: 100");
            return 100; // Default si hay error de formato
        }
    }

    /**
     * Simulación paralela que soporta tanto SJF como Round Robin
     * CORREGIDO: Variable quantum ahora es final
     */
    private void ejecutarSimulacionParalela() {
        List<Proceso> listaProcesos = new ArrayList<>(procesos);
        String algoritmoSeleccionado = comboAlgoritmo.getValue();

        // CORREGIDO: Inicializar quantum directamente
        final int quantum = "Round Robin".equals(algoritmoSeleccionado) ?
                obtenerQuantumValue() : 0;

        if ("Round Robin".equals(algoritmoSeleccionado)) {
            System.out.println("🔄 Round Robin iniciado con quantum=" + quantum);
        } else {
            System.out.println("🎯 SJF iniciado");
        }

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
        Map<Integer, Proceso> procesosEnCores = new HashMap<>();
        Map<Proceso, Integer> tiempoRestanteProceso = new HashMap<>();

<<<<<<< HEAD
        // Inicializar todos los procesos como NUEVO
=======
        // NUEVO: Map para tracking de quantum (solo para Round Robin)
        Map<Proceso, Integer> quantumRestanteProceso = new HashMap<>();

        // Inicializar tiempos restantes
>>>>>>> origin/Benja
        for (Proceso p : listaProcesos) {
            actualizarEstadoProceso(p, EstadoProceso.NUEVO);
            tiempoRestanteProceso.put(p, p.getDuracion());
        }

        // Actualizar tabla inicial
        actualizarTablaColas();

        Platform.runLater(this::prepararGantt);

        Thread simulacionThread = new Thread(() -> {
            try {
                tiempoActual = 0;

                while (corriendo && (!procesosPendientes.isEmpty() || !procesosListos.isEmpty() ||
                        !procesosEnCores.isEmpty() || !memoria.getSwapping().estaVacio())) {

                    while (pausado && corriendo) {
                        Thread.sleep(100);
                    }

                    if (!corriendo) break;

                    // Estadísticas cada 50 unidades para no saturar
                    if (tiempoActual % 50 == 0) {
                        System.out.println("📊 t=" + tiempoActual + " - Pendientes: " + procesosPendientes.size() +
                                ", Listos: " + procesosListos.size() +
                                ", En ejecución: " + procesosEnCores.size() +
                                ", En swap: " + memoria.getSwapping().getCantidadProcesos());
                    }

                    // 1. Verificar llegadas de procesos
                    procesosPendientes.removeIf(proceso -> {
                        if (proceso.getTiempoLlegada() <= tiempoActual) {
                            if (memoria.asignarMemoria(proceso)) {
                                procesosListos.add(proceso);
                                actualizarEstadoProceso(proceso, EstadoProceso.LISTO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó y obtuvo memoria");
                            } else {
                                memoria.moverASwapping(proceso);
<<<<<<< HEAD
                                actualizarEstadoProceso(proceso, EstadoProceso.ESPERANDO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó pero fue a SWAP");
                            }

=======
                                proceso.setEstado(EstadoProceso.ESPERANDO);
                                System.out.println("⏰ t=" + tiempoActual + ": Proceso " + proceso.getId() + " llegó pero fue a SWAP");
                            }
>>>>>>> origin/Benja
                            actualizarVisualizacionMemoria();
                            return true;
                        }
                        return false;
                    });

<<<<<<< HEAD
                    // 2. Asignar procesos a cores libres usando SJF
                    if (!procesosListos.isEmpty()) {
                        procesosListos.sort((p1, p2) -> Integer.compare(
                                tiempoRestanteProceso.get(p1),
                                tiempoRestanteProceso.get(p2)
                        ));

                        for (int coreId = 0; coreId < 5 && !procesosListos.isEmpty(); coreId++) {
                            if (!procesosEnCores.containsKey(coreId)) {
                                Proceso proceso = procesosListos.remove(0);
                                procesosEnCores.put(coreId, proceso);
                                actualizarEstadoProceso(proceso, EstadoProceso.EJECUTANDO);
                                System.out.println("🔧 t=" + tiempoActual + ": Core-" + coreId + " ejecuta Proceso " + proceso.getId());
                            }
                        }
=======
                    // 2. Intentar mover procesos de SWAP a RAM
                    List<Proceso> procesosMovidosDeSwap = memoria.getSwapping().procesarCola(memoria);
                    for (Proceso proceso : procesosMovidosDeSwap) {
                        procesosListos.add(proceso);
                        System.out.println("🔄 t=" + tiempoActual + ": Proceso " + proceso.getId() + " movido de SWAP a RAM");
                    }
                    if (!procesosMovidosDeSwap.isEmpty()) {
                        actualizarVisualizacionMemoria();
>>>>>>> origin/Benja
                    }

                    // 3. EJECUTAR PROCESOS EN CORES (MODIFICADO PARA SOPORTAR ROUND ROBIN)
                    List<Integer> coresALiberar = new ArrayList<>();
                    for (Map.Entry<Integer, Proceso> entry : procesosEnCores.entrySet()) {
                        int coreId = entry.getKey();
                        Proceso proceso = entry.getValue();

                        // Obtener tiempos actuales
                        int tiempoRestante = tiempoRestanteProceso.get(proceso);
                        Integer quantumRestante = quantumRestanteProceso.get(proceso);

                        // PRINT CADA 20 UNIDADES para debugging
                        if (tiempoActual % 20 == 0) {
                            if ("Round Robin".equals(algoritmoSeleccionado)) {
                                System.out.printf("🔧 t=%d: Core-%d ejecuta Proceso %d (CPU restante: %d, quantum restante: %d)%n",
                                        tiempoActual, coreId, proceso.getId(), tiempoRestante, quantumRestante != null ? quantumRestante : 0);
                            } else {
                                System.out.printf("🔧 t=%d: Core-%d ejecuta Proceso %d (tiempo restante: %d)%n",
                                        tiempoActual, coreId, proceso.getId(), tiempoRestante);
                            }
                        }

                        // Visualizar en Gantt
                        final int finalTiempo = tiempoActual;
                        final int finalCore = coreId;
                        Platform.runLater(() -> {
                            Color color = proceso.getColor() != null ? proceso.getColor() : Color.LIGHTBLUE;
                            pintarCeldaCore(finalCore, finalTiempo, color);
                        });

                        // Ejecutar por 1 unidad de tiempo
                        tiempoRestante--;
                        tiempoRestanteProceso.put(proceso, tiempoRestante);

                        // ROUND ROBIN: Decrementar quantum
                        if ("Round Robin".equals(algoritmoSeleccionado) && quantumRestante != null) {
                            quantumRestante--;
                            quantumRestanteProceso.put(proceso, quantumRestante);
                        }

                        // Verificar condiciones de terminación/pausa
                        if (tiempoRestante <= 0) {
<<<<<<< HEAD
                            actualizarEstadoProceso(proceso, EstadoProceso.TERMINADO);
=======
                            // Proceso terminado
                            proceso.setEstado(EstadoProceso.TERMINADO);
>>>>>>> origin/Benja
                            coresALiberar.add(coreId);
                            memoria.liberarMemoria(proceso);
<<<<<<< HEAD

                            System.out.println("✅ t=" + tiempoActual + ": Proceso " + proceso.getId() + " terminado en Core-" + coreId);

=======
                            System.out.println("✅ t=" + tiempoActual + ": Proceso " + proceso.getId() + " TERMINADO en Core-" + coreId);
>>>>>>> origin/Benja
                            actualizarVisualizacionMemoria();
                        }
                        // ROUND ROBIN: Verificar si se agotó el quantum (SIN terminar el proceso)
                        else if ("Round Robin".equals(algoritmoSeleccionado) && quantumRestante != null && quantumRestante <= 0) {
                            // Quantum agotado - hacer cambio de contexto
                            proceso.setEstado(EstadoProceso.LISTO);
                            procesosListos.add(proceso);
                            coresALiberar.add(coreId);

                            // PRINT CLAVE para verificar Round Robin
                            System.out.println("🔄 t=" + tiempoActual + ": Proceso " + proceso.getId() +
                                    " PAUSADO por quantum en Core-" + coreId +
                                    " (CPU restante: " + tiempoRestante + ")");
                        }
                    }

                    // 4. Liberar cores
                    for (Integer coreId : coresALiberar) {
                        procesosEnCores.remove(coreId);
                    }

<<<<<<< HEAD
                    // 5. Actualizar tabla de colas cada ciclo
                    actualizarTablaColas();

                    // 6. Avanzar tiempo y esperar
=======
                    // 5. ASIGNAR PROCESOS A CORES LIBRES
                    if (!procesosListos.isEmpty()) {
                        // ALGORITMO DE ORDENAMIENTO SEGÚN TIPO SELECCIONADO
                        if ("Round Robin".equals(algoritmoSeleccionado)) {
                            // Round Robin: FCFS (no reordenar, usar orden de llegada a la cola)
                            // No hacer nada - mantener orden FIFO
                        } else {
                            // SJF: Ordenar por tiempo restante
                            procesosListos.sort((p1, p2) -> Integer.compare(
                                    tiempoRestanteProceso.get(p1),
                                    tiempoRestanteProceso.get(p2)
                            ));
                        }

                        // Asignar a cores disponibles
                        for (int coreId = 0; coreId < NUMERO_CORES && !procesosListos.isEmpty(); coreId++) {
                            if (!procesosEnCores.containsKey(coreId)) {
                                Proceso proceso = procesosListos.remove(0);
                                procesosEnCores.put(coreId, proceso);
                                proceso.setEstado(EstadoProceso.EJECUTANDO);

                                // ROUND ROBIN: Reiniciar quantum al asignar
                                if ("Round Robin".equals(algoritmoSeleccionado)) {
                                    quantumRestanteProceso.put(proceso, quantum);
                                    System.out.println("🔧 t=" + tiempoActual + ": Core-" + coreId +
                                            " ASIGNADO a Proceso " + proceso.getId() +
                                            " (CPU restante: " + tiempoRestanteProceso.get(proceso) +
                                            ", quantum: " + quantum + ")");
                                } else {
                                    System.out.println("🔧 t=" + tiempoActual + ": Core-" + coreId + " ejecuta Proceso " + proceso.getId() +
                                            " (tiempo restante: " + tiempoRestanteProceso.get(proceso) + ")");
                                }
                            }
                        }
                    }

                    // 6. ESTADO DE QUANTUM CADA 100 UNIDADES (solo Round Robin)
                    if ("Round Robin".equals(algoritmoSeleccionado) && tiempoActual % 100 == 0) {
                        System.out.println("\n=== ESTADO QUANTUM t=" + tiempoActual + " ===");
                        for (Map.Entry<Integer, Proceso> entry : procesosEnCores.entrySet()) {
                            int coreId = entry.getKey();
                            Proceso p = entry.getValue();
                            Integer qRestante = quantumRestanteProceso.get(p);
                            System.out.printf("Core-%d: Proceso %d (quantum restante: %d)%n",
                                    coreId, p.getId(), qRestante != null ? qRestante : 0);
                        }
                        System.out.println("================================\n");
                    }

                    // 7. Avanzar tiempo
>>>>>>> origin/Benja
                    tiempoActual++;
                    Thread.sleep(STEP_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                Platform.runLater(() -> {
                    corriendo = false;
                    actualizarTablaColas(); // Actualización final
                    System.out.println("🏁 Simulación terminada en t=" + tiempoActual);
                    System.out.println("📈 Procesos completados: " +
                            listaProcesos.stream().mapToInt(p -> p.getEstado() == EstadoProceso.TERMINADO ? 1 : 0).sum() +
                            "/" + listaProcesos.size());
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

        System.out.println("🚀 Simulación paralela iniciada con " + procesos.size() + " procesos en " + NUMERO_CORES + " cores");
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
            limpiarTablaColas(); // LÍNEA AGREGADA

            if (memoria != null) {
                memoria.reiniciar();
                actualizarVisualizacionMemoria();
            }

            if (procesos.isEmpty()) {
<<<<<<< HEAD
=======
                // Restaurar colores disponibles (incluyendo el nuevo color BROWN para 6 cores)
>>>>>>> origin/Benja
                coloresDisponibles.clear();
                coloresDisponibles.addAll(Arrays.asList(
                        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.BROWN
                ));
                crearProcesosPredefinidos();
            }

            // Actualizar tabla después de recrear procesos
            actualizarTablaColas(); // LÍNEA AGREGADA
        });

        System.out.println("🔄 Simulación reiniciada con " + NUMERO_CORES + " cores");
    }

    private void inicializarTablaColas() {
        // Inicializar datos de la tabla
        datosTablaEstados = FXCollections.observableArrayList();

        // Configurar las columnas de la tabla
        colNuevo.setCellValueFactory(new PropertyValueFactory<>("nuevo"));
        colListo.setCellValueFactory(new PropertyValueFactory<>("listo"));
        colEspera.setCellValueFactory(new PropertyValueFactory<>("espera"));
        colTerminado.setCellValueFactory(new PropertyValueFactory<>("terminado"));

        // Asignar los datos a la tabla
        tablaColas.setItems(datosTablaEstados);

        // Crear primera fila vacía
        actualizarTablaColas();

        System.out.println("📋 Tabla de colas inicializada");
    }
    private void actualizarTablaColas() {
        Platform.runLater(() -> {
            // Limpiar listas actuales
            procesosNuevo.clear();
            procesosListo.clear();
            procesosEspera.clear();
            procesosTerminado.clear();

            // Clasificar procesos por estado
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
                        // Los procesos ejecutándose se consideran "Listo" para efectos de visualización
                        procesosListo.add(proceso);
                        break;
                }
            }

            // Limpiar tabla actual
            datosTablaEstados.clear();

            // Determinar el número máximo de filas necesarias
            int maxFilas = Math.max(Math.max(procesosNuevo.size(), procesosListo.size()),
                    Math.max(procesosEspera.size(), procesosTerminado.size()));

            // Si no hay procesos, mostrar al menos una fila vacía
            if (maxFilas == 0) {
                maxFilas = 1;
            }

            // Crear filas para la tabla usando FilaEstadoProcesos
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

        // Agregar información adicional según el estado
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

    /**
     * Actualiza el estado de un proceso específico y refresca la tabla
     */
    private void actualizarEstadoProceso(Proceso proceso, EstadoProceso nuevoEstado) {
        if (proceso != null && proceso.getEstado() != nuevoEstado) {
            EstadoProceso estadoAnterior = proceso.getEstado();
            proceso.setEstado(nuevoEstado);

            System.out.println("🔄 Proceso " + proceso.getId() +
                    " cambió de " + estadoAnterior + " a " + nuevoEstado);

            // Actualizar tabla inmediatamente
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

            // Agregar fila vacía
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

}