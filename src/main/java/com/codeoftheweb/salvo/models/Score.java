package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Score {
    /* ======================= ATTRIBUTES ======================= */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private double score;
    private Date finishDate;

    /* Method where I create a -Many to one- relationship between Score and Game */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_id")
    private Game game;

    /* Method where I create a -Many to one- relationship between Score and Player */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    /* ======================= CONSTRUCTOR ======================= */
    public Score() {
    }

    public Score(Game game, Player player, double score, Date finishDate) {
        this.game = game;
        this.player = player;
        this.score = score;
        this.finishDate = new Date();
    }

    /* ======================= Getters ======================= */
    public long getId() {
        return id;
    }

    public double getScore() {
        return score;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    /* ======================= Setters ======================= */
    public void setId(long id) {
        this.id = id;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    /* ======================= DTO ======================= */
    public Map<String, Object> scoreDTO() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("player", this.getPlayer().getId());
        dto.put("score", this.getScore());
        dto.put("finishDate", this.getFinishDate());
        return dto;
    }
}