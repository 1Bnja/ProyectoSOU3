package org.example.proyectoso.memoria;

import org.example.proyectoso.models.Proceso;

public class BloqueMemoria {
    private int inicio;
    private int tamaño;
    private boolean ocupado;
    private Proceso proceso;

    public BloqueMemoria(int inicio, int tamaño) {
        this.inicio = inicio;
        this.tamaño = tamaño;
        this.ocupado = false;
        this.proceso = null;
    }


    public void asignar(Proceso proceso) {
        this.ocupado = true;
        this.proceso = proceso;
    }


    public void liberar() {
        this.ocupado = false;
        this.proceso = null;
    }


    public int getFin() {
        return inicio + tamaño - 1;
    }


    public boolean esAdyacente(BloqueMemoria otro) {
        return (this.getFin() + 1 == otro.getInicio()) ||
                (otro.getFin() + 1 == this.getInicio());
    }


    public boolean puedeUnirse(BloqueMemoria otro) {
        return !this.ocupado && !otro.ocupado && this.esAdyacente(otro);
    }

    
    public int getInicio() {
        return inicio;
    }

    public int getTamaño() {
        return tamaño;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public Proceso getProceso() {
        return proceso;
    }


    public void setTamaño(int tamaño) {
        this.tamaño = tamaño;
    }

    public void setInicio(int inicio) {
        this.inicio = inicio;
    }


    @Override
    public String toString() {
        if (ocupado) {
            return String.format("Bloque[%d-%d, %dMB, Proceso %d]",
                    inicio, getFin(), tamaño, proceso.getId());
        } else {
            return String.format("Bloque[%d-%d, %dMB, LIBRE]",
                    inicio, getFin(), tamaño);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BloqueMemoria bloque = (BloqueMemoria) obj;
        return inicio == bloque.inicio && tamaño == bloque.tamaño;
    }

    @Override
    public int hashCode() {
        return inicio * 31 + tamaño;
    }
}