package com.pruebaapi.Model;

import com.google.gson.Gson;

public class Tablero {
    private String idTablero;
    private Usuario usuario;
    private String casillas;

    public Tablero() {
    }

    public Tablero(String idTablero) {
        this.idTablero = idTablero;
    }

    public String getIdTablero() {
        return idTablero;
    }

    public void setIdTablero(String idTablero) {
        this.idTablero = idTablero;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCasillas() {
        return casillas;
    }

    public void setCasillas(String casillas) {
        this.casillas = casillas;
    }

    public void setCasillasFromArray(int[][] casillasArray) {
        this.casillas = new Gson().toJson(casillasArray);
    }
}
