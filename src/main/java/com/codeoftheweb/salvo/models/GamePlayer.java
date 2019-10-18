package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;


@Entity
public class GamePlayer {
    /* ======================= ATTRIBUTES ======================= */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private Date joinDate;

    /* Method where I create a -Many to one- relationship between GamePlayer and Player */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    /* Method where I create a -Many to one- relationship between GamePlayer and Game */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    /* Method where I create a -One to many- relationship between GamePlayer and Ship */
    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
    private Set<Ship> ships;

    /* Method where I create a -One to many- relationship between GamePlayer and Salvo */
    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
    private Set<Salvo> salvoes;


    /* ======================= CONSTRUCTOR ======================= */
    public GamePlayer() {
    }

    public GamePlayer(Date joinDate, Game game, Player player) {
        this.joinDate = joinDate;
        this.game = game;
        this.player = player;
    }

    public GamePlayer(Game game, Player player) {
        this.joinDate = new Date();
        this.game = game;
        this.player = player;
    }

    /* ======================= Getters ======================= */
    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public Game getGame() {
        return game;
    }

    public Set<Ship> getShips() {
        return ships;
    }

    public Set<Salvo> getSalvoes() {
        return this.salvoes;
    }


    /* ======================= Setters ======================= */
    public void setId(long id) {
        this.id = id;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public void setSalvos(Set<Salvo> salvoes) {
        this.salvoes = salvoes;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    /* Methods */
    public void addShip(Ship ship) {
        this.ships.add(ship);
        ship.setGamePlayer(this);
    }

    public void addSalvo(Salvo salvo) {
        this.salvoes.add(salvo);
        salvo.setGamePlayer(this);
    }

    /* =======================  DTO ======================= */
    public Map<String, Object> getGamePlayerDTO() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", this.getId());
        dto.put("player", this.getPlayer().getPlayerDTO());
        dto.put("joinDate", this.getJoinDate());
        return dto;
    }
}






