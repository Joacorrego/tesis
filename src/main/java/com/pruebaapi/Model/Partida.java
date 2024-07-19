package com.pruebaapi.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Partida {

    private String idPartida;
    private String fecha;
    private Usuario jugador1;
    private Usuario jugador2;
    private Tablero tablero1;
    private Tablero tablero2;
    private int puntuacionJ1;
    private int puntuacionJ2;
    private int turno;
    private String codigoPartida;

    public Partida() {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        this.fecha = formatoFecha.format(new Date());
        this.puntuacionJ1 = 0;
        this.puntuacionJ2 = 0;
        this.turno = new Random().nextInt(2) + 1;
    }

    public String getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(String idPartida) {
        this.idPartida = idPartida;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Usuario getJugador1() {
        return jugador1;
    }

    public void setJugador1(Usuario jugador1) {
        this.jugador1 = jugador1;
    }

    public Usuario getJugador2() {
        return jugador2;
    }

    public void setJugador2(Usuario jugador2) {
        this.jugador2 = jugador2;
    }

    public Tablero getTablero1() {
        return tablero1;
    }

    public void setTablero1(Tablero tablero1) {
        this.tablero1 = tablero1;
    }

    public Tablero getTablero2() {
        return tablero2;
    }

    public void setTablero2(Tablero tablero2) {
        this.tablero2 = tablero2;
    }

    public int getPuntuacionJ1() {
        return puntuacionJ1;
    }

    public void setPuntuacionJ1(int puntuacionJ1) {
        this.puntuacionJ1 = puntuacionJ1;
    }

    public int getPuntuacionJ2() {
        return puntuacionJ2;
    }

    public void setPuntuacionJ2(int puntuacionJ2) {
        this.puntuacionJ2 = puntuacionJ2;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public String getCodigoPartida() {
        return codigoPartida;
    }

    public void setCodigoPartida(String codigoPartida) {
        this.codigoPartida = codigoPartida;
    }
}
