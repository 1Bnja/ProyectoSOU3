package org.example.proyectoso.models;

public class Proceso {
    private int id;
    private int tiempoLlegada;
    private int duracion;
    private int tamanoMemoria;
    private EstadoProceso estado;
    private int tiempoRestante;
    private int tiempoEspera;
    private int tiempoRespuesta;

    public Proceso(int id, int tiempoLlegada, int duracion, int tamanoMemoria) {
        this.id = id;
        this.tiempoLlegada = tiempoLlegada;
        this.duracion = duracion;
        this.tamanoMemoria = tamanoMemoria;
        this.estado = EstadoProceso.NUEVO;
        this.tiempoRestante = duracion;
        this.tiempoEspera = 0;
        this.tiempoRespuesta = 0;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public int getId() {
        return id;
    }
    public int getTiempoLlegada() {
        return tiempoLlegada;
    }
    public EstadoProceso getEstado() {
        return estado;
    }
    public int getTiempoRestante() {
        return tiempoRestante;
    }
    public void decrementarTiempo() {
        if (tiempoRestante > 0) {
            tiempoRestante--;
        }
    }
    public int getDuracion() {
        return duracion;
    }
    public int getTamanoMemoria() {
        return tamanoMemoria;
    }
}
