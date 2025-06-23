package org.example.proyectoso.models;

import javafx.scene.paint.Color;

public class Proceso {
    // Identificación
    private static int contadorId = 1;
    private final int id;
    private final String nombre;

    // Características del proceso
    private final int duracion;
    private final int tamanoMemoria;
    private final int tiempoLlegada;
    private int tiempoEspera;
    private int tiempoRespuesta;
    private int tiempoRetorno;

    // Estado y ejecución
    private EstadoProceso estado;
    private int tiempoEjecutado;
    private int tiempoRestante;
    private long tiempoInicioEjecucion;
    private long tiempoFinalizacion;

    // NUEVAS VARIABLES AGREGADAS:
    private boolean yaComenzo = false;        // Para saber si ya empezó alguna vez
    private int tiempoInicioReal = -1;        // Primer momento que comenzó a ejecutarse
    private int tiempoFinalizacionReal = -1;  // Momento que terminó completamente

    // Para Round Robin
    private int quantumRestante;

    // Color distintivo para la visualización
    private Color color;

    public Proceso(String nombre, int duracion, int tamanoMemoria, int tiempoLlegada) {
        this.id = contadorId++;
        this.nombre = nombre;
        this.duracion = duracion;
        this.tamanoMemoria = tamanoMemoria;
        this.tiempoLlegada = tiempoLlegada;

        // Inicializar estado
        this.estado = EstadoProceso.NUEVO;
        this.tiempoEjecutado = 0;
        this.tiempoRestante = duracion;
        this.tiempoEspera = 0;
        this.tiempoRespuesta = -1; // -1 indica que no ha comenzado
        this.tiempoRetorno = 0;
        this.quantumRestante = 0;
        this.tiempoInicioEjecucion = -1;
        this.tiempoFinalizacion = -1;

        // NUEVAS INICIALIZACIONES:
        this.yaComenzo = false;
        this.tiempoInicioReal = -1;
        this.tiempoFinalizacionReal = -1;
    }

    public Proceso(String nombre, int duracion, int tamanoMemoria) {
        this(nombre, duracion, tamanoMemoria, 0);
    }

    // MÉTODO MODIFICADO:
    public void iniciarEjecucion(long tiempoActual) {
        if (estado == EstadoProceso.LISTO || estado == EstadoProceso.NUEVO) {
            estado = EstadoProceso.EJECUTANDO;

            // Si es la primera vez que se ejecuta, calcular tiempo de respuesta
            if (tiempoRespuesta == -1) {
                tiempoRespuesta = (int) (tiempoActual - tiempoLlegada);
                tiempoInicioEjecucion = tiempoActual;
            }
        }
    }

    // NUEVOS MÉTODOS AGREGADOS:
    /**
     * Marca que el proceso comenzó a ejecutarse por primera vez
     */
    public void marcarInicioEjecucion(int tiempoActual) {
        if (!yaComenzo) {
            this.tiempoInicioReal = tiempoActual;
            this.yaComenzo = true;
            System.out.println("🎬 Proceso " + this.getId() + " comenzó por primera vez en t=" + tiempoActual);
        }
    }

    /**
     * Marca que el proceso terminó completamente
     */
    public void marcarFinalizacion(int tiempoActual) {
        this.tiempoFinalizacionReal = tiempoActual;
        System.out.println("🏁 Proceso " + this.getId() + " terminó en t=" + tiempoActual);
    }

    /**
     * Reinicia los tiempos para una nueva simulación
     */
    public void reiniciarTiempos() {
        this.tiempoInicioReal = -1;
        this.tiempoFinalizacionReal = -1;
        this.yaComenzo = false;
        this.estado = EstadoProceso.NUEVO;
        this.tiempoEjecutado = 0;
        this.tiempoRestante = duracion;
        this.tiempoEspera = 0;
        this.tiempoRespuesta = -1;
        this.tiempoRetorno = 0;
        this.quantumRestante = 0;
        this.tiempoInicioEjecucion = -1;
        this.tiempoFinalizacion = -1;
    }

    public boolean ejecutar(int quantum, long tiempoActual) {
        if (estado != EstadoProceso.EJECUTANDO) {
            return false;
        }

        quantumRestante = quantum;
        int tiempoAEjecutar = Math.min(quantum, tiempoRestante);

        // Simular ejecución
        try {
            Thread.sleep(tiempoAEjecutar);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Actualizar tiempos
        tiempoEjecutado += tiempoAEjecutar;
        tiempoRestante -= tiempoAEjecutar;
        quantumRestante -= tiempoAEjecutar;

        // Verificar si terminó
        if (tiempoRestante <= 0) {
            finalizar(tiempoActual + tiempoAEjecutar);
            return true;
        }

        return false;
    }

    public void pausar() {
        if (estado == EstadoProceso.EJECUTANDO) {
            estado = EstadoProceso.LISTO;
        }
    }

    public void bloquear() {
        estado = EstadoProceso.ESPERANDO;
    }

    public void desbloquear() {
        if (estado == EstadoProceso.ESPERANDO) {
            estado = EstadoProceso.LISTO;
        }
    }

    public void finalizar(long tiempoActual) {
        estado = EstadoProceso.TERMINADO;
        tiempoFinalizacion = tiempoActual;
        tiempoRetorno = (int) (tiempoFinalizacion - tiempoLlegada);

        System.out.println("✅ Proceso " + id + " (" + nombre + ") TERMINADO");
    }

    public void actualizarTiempoEspera(int incremento) {
        if (estado == EstadoProceso.LISTO || estado == EstadoProceso.ESPERANDO) {
            tiempoEspera += incremento;
        }
    }

    public double getPorcentajeCompletitud() {
        return (double) tiempoEjecutado / duracion * 100;
    }

    public boolean haTerminado() {
        return estado == EstadoProceso.TERMINADO;
    }

    public boolean estaEjecutando() {
        return estado == EstadoProceso.EJECUTANDO;
    }

    public boolean estaListo() {
        return estado == EstadoProceso.LISTO;
    }

    public boolean estaEsperando() {
        return estado == EstadoProceso.ESPERANDO;
    }

    // GETTERS MODIFICADOS PARA CÁLCULOS CORRECTOS:
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDuracion() {
        return duracion;
    }

    public int getTamanoMemoria() {
        return tamanoMemoria;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    /**
     * Calcula el tiempo de espera correctamente
     * Tiempo de espera = (Tiempo de inicio de ejecución) - (Tiempo de llegada)
     */
    public int getTiempoEspera() {
        if (tiempoInicioReal == -1) {
            return 0; // Aún no ha comenzado
        }
        return Math.max(0, tiempoInicioReal - tiempoLlegada);
    }

    /**
     * Calcula el tiempo de respuesta correctamente
     * Tiempo de respuesta = (Tiempo de inicio de ejecución) - (Tiempo de llegada)
     */
    public int getTiempoRespuesta() {
        if (tiempoInicioReal == -1) {
            return -1; // Aún no ha comenzado
        }
        return Math.max(0, tiempoInicioReal - tiempoLlegada);
    }

    /**
     * Calcula el tiempo de retorno (turnaround time) correctamente
     * Tiempo de retorno = (Tiempo de finalización) - (Tiempo de llegada)
     */
    public int getTiempoRetorno() {
        if (tiempoFinalizacionReal == -1) {
            return 0; // Aún no ha terminado
        }
        return Math.max(0, tiempoFinalizacionReal - tiempoLlegada);
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public int getTiempoEjecutado() {
        return tiempoEjecutado;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public int getQuantumRestante() {
        return quantumRestante;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public long getTiempoInicioEjecucion() {
        return tiempoInicioEjecucion;
    }

    public long getTiempoFinalizacion() {
        return tiempoFinalizacion;
    }

    // NUEVOS GETTERS:
    public int getTiempoInicioReal() {
        return tiempoInicioReal;
    }

    public int getTiempoFinalizacionReal() {
        return tiempoFinalizacionReal;
    }

    public boolean yaComenzo() {
        return yaComenzo;
    }

    @Override
    public String toString() {
        return String.format("Proceso[ID:%d, %s, %s, %d/%dms, %dMB, %.1f%%]",
                id, nombre, estado, tiempoEjecutado, duracion,
                tamanoMemoria, getPorcentajeCompletitud());
    }

    public String getInformacionDetallada() {
        return String.format(
                "Proceso %d (%s):\n" +
                        "  Estado: %s\n" +
                        "  Duración: %d ms\n" +
                        "  Ejecutado: %d ms (%.1f%%)\n" +
                        "  Restante: %d ms\n" +
                        "  Memoria: %d MB\n" +
                        "  Llegada: %d ms\n" +
                        "  Espera: %d ms\n" +
                        "  Respuesta: %d ms\n" +
                        "  Retorno: %d ms\n" +
                        "  Inicio real: %d\n" +
                        "  Fin real: %d",
                id, nombre, estado, duracion, tiempoEjecutado,
                getPorcentajeCompletitud(), tiempoRestante, tamanoMemoria,
                tiempoLlegada, getTiempoEspera(), getTiempoRespuesta(), getTiempoRetorno(),
                tiempoInicioReal, tiempoFinalizacionReal
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Proceso proceso = (Proceso) obj;
        return id == proceso.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}