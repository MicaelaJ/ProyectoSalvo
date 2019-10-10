package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
public class Game {

    /* ======================= Atributos ======================= */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private Date creationDate;
    private double score;

    /* Metodo donde creo una relacion One to many entre Game y GamePlayer */
    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    private Set<GamePlayer> gamePlayers;

    /* Metodo donde creo una relacion One to many entre Game y Score */
    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    private Set<Score> scores;

    /* ======================= Constructor ======================= */
    public Game() {
    }

    /* Declaracion de metodo */
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
}


