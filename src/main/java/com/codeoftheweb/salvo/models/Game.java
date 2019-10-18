package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Game {

    /* ======================= ATTRIBUTES ======================= */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private Date creationDate;
    private double score;

    /* Method where I create a -One to many- relationship between Game and GamePlayer */
    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    private Set<GamePlayer> gamePlayers;

    /* Method where I create a -One to many- relationship between Game and Score */
    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    private Set<Score> scores;

    public List<String> getAllUserNames() {
        return gamePlayers.stream().map(game -> game.getPlayer().getUserName()).collect(Collectors.toList());
    }

    /* ======================= CONSTRUCTOR ======================= */
    public Game() {
    }

    // Method declaration
    public Game(Date creationDate) {
        this.creationDate = creationDate;
    }

    /* ======================= Getters ======================= */
    public long getId() {
        return id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Double getScore() {
        return score;
    }

    public Set<Score> getScores() {
        return scores;
    }

    /* ======================= Setters ======================= */
    public void setId(long id) {
        this.id = id;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setScores() {
        this.scores = scores;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

