package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Salvo {

    /* ======================= Atributos ======================= */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private int turn;

    /* Metodo donde creo una relacion One to many entre Salvo y GamePlayer */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_id")
    private GamePlayer gamePlayer;

    /* Metodo para SalvoLocations */
    @ElementCollection
    @Column(name = "salvoLocations")
    private Set<String> salvoLocations = new HashSet<>();


    /* ======================= Salvo Location ======================= */

    private List<Map<String, Object>> getAllSalvos(Set<Salvo> salvos) {
        return salvos
                .stream()
                .map(salvo -> salvo.salvoDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Constructor ======================= */

    public Salvo() {
    }

    public Salvo(int turn, GamePlayer gameplayer, Set<String> salvoLocations) {
        this.turn = turn;
        this.gamePlayer = gameplayer;
        this.salvoLocations = salvoLocations;
    }

    /* ======================= Getters ======================= */

    public long getId() {
        return id;
    }

    public int getTurn() {
        return turn;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public GamePlayer getSalvoPlayer() {
        return this.getGamePlayer();
    }

    public Set<String> getSalvoLocations() {
        return salvoLocations;
    }

    /* ======================= Setters ======================= */

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }


    public void setSalvoLocations(Set<String> salvoLocations) {
        this.salvoLocations = salvoLocations;
    }

    /* ======================= DTO ======================= */

    public Map<String, Object> salvoDTO() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("turn", this.getTurn());
        dto.put("player", this.getGamePlayer().getPlayer().getId());
        dto.put("locations", this.getSalvoLocations());
        return dto;
    }
}
