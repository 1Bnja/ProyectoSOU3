package org.example.proyectoso.models;

import javafx.scene.paint.Color;

public class Proceso {
    
    private static int contadorId = 1;
    private final int id;
    private final String nombre;

    
    private final int duracion;
    private final int tamanoMemoria;
    private final int tiempoLlegada;
    private int tiempoEspera;
    private int tiempoRespuesta;
    private int tiempoRetorno;

    
    private EstadoProceso estado;
    private int tiempoEjecutado;
    private int tiempoRestante;
    private long tiempoInicioEjecucion;
    private long tiempoFinalizacion;

    
    private boolean yaComenzo = false;        
    private int tiempoInicioReal = -1;        
    private int tiempoFinalizacionReal = -1;  

    
    private int quantumRestante;

    
    private Color color;

    public Proceso(String nombre, int duracion, int tamanoMemoria, int tiempoLlegada) {
        this.id = contadorId++;
        this.nombre = nombre;
        this.duracion = duracion;
        this.tamanoMemoria = tamanoMemoria;
        this.tiempoLlegada = tiempoLlegada;

        
        this.estado = EstadoProceso.NUEVO;
        this.tiempoEjecutado = 0;
        this.tiempoRestante = duracion;
        this.tiempoEspera = 0;
        this.tiempoRespuesta = -1; 
        this.tiempoRetorno = 0;
        this.quantumRestante = 0;
        this.tiempoInicioEjecucion = -1;
        this.tiempoFinalizacion = -1;

        
        this.yaComenzo = false;
        this.tiempoInicioReal = -1;
        this.tiempoFinalizacionReal = -1;
    }

    public Proceso(String nombre, int duracion, int tamanoMemoria) {
        this(nombre, duracion, tamanoMemoria, 0);
    }

    
    public void iniciarEjecucion(long tiempoActual) {
        if (estado == EstadoProceso.LISTO || estado == EstadoProceso.NUEVO) {
            estado = EstadoProceso.EJECUTANDO;

            
            if (tiempoRespuesta == -1) {
                tiempoRespuesta = (int) (tiempoActual - tiempoLlegada);
                tiempoInicioEjecucion = tiempoActual;
            }
        }
    }

    
    
    public void marcarInicioEjecucion(int tiempoActual) {
        if (!yaComenzo) {
            this.tiempoInicioReal = tiempoActual;
            this.yaComenzo = true;
            System.out.println("🎬 Proceso " + this.getId() + " comenzó por primera vez en t=" + tiempoActual);
        }
    }

    
    public void marcarFinalizacion(int tiempoActual) {
        this.tiempoFinalizacionReal = tiempoActual;
        System.out.println("🏁 Proceso " + this.getId() + " terminó en t=" + tiempoActual);
    }

    
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

        
        try {
            Thread.sleep(tiempoAEjecutar);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        
        tiempoEjecutado += tiempoAEjecutar;
        tiempoRestante -= tiempoAEjecutar;
        quantumRestante -= tiempoAEjecutar;

        
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


    public void finalizar(long tiempoActual) {
        estado = EstadoProceso.TERMINADO;
        tiempoFinalizacion = tiempoActual;
        tiempoRetorno = (int) (tiempoFinalizacion - tiempoLlegada);

        System.out.println("✅ Proceso " + id + " (" + nombre + ") TERMINADO");
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

    
    public int getTiempoEspera() {
        if (tiempoInicioReal == -1) {
            return 0; 
        }
        return Math.max(0, tiempoInicioReal - tiempoLlegada);
    }

    
    public int getTiempoRespuesta() {
        if (tiempoInicioReal == -1) {
            return -1; 
        }
        return Math.max(0, tiempoInicioReal - tiempoLlegada);
    }

    
    public int getTiempoRetorno() {
        if (tiempoFinalizacionReal == -1) {
            return 0; 
        }
        return Math.max(0, tiempoFinalizacionReal - tiempoLlegada);
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }



    public int getTiempoRestante() {
        return tiempoRestante;
    }



    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }





    
    public int getTiempoInicioReal() {
        return tiempoInicioReal;
    }

    public int getTiempoFinalizacionReal() {
        return tiempoFinalizacionReal;
    }



    @Override
    public String toString() {
        return String.format("Proceso[ID:%d, %s, %s, %d/%dms, %dMB, %.1f%%]",
                id, nombre, estado, tiempoEjecutado, duracion,
                tamanoMemoria, getPorcentajeCompletitud());
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