package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Player {
    /* ======================= ATTRIBUTES ======================= */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private String userName;
    private String password;

    /* Method where I create a -One to many- relationship between Player and GamePlayer */
    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER)
    private Set<GamePlayer> gamePlayers;

    /* Method where I create a -One to many- relationship between Player and GamePlayer */
    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER)
    private Set<Score> scores;

    /* ======================= CONSTRUCTOR ======================= */
    public Player() {
    }

    public Player(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /* ======================= Getters ======================= */
    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public Set<Score> getScores() {
        return (Set<Score>) scores;
    }

    public String getPassword() {
        return password;
    }

    public List<Game> getGames() {
        return this.gamePlayers
                .stream()
                .map(GamePlayer::getGame)
                .collect(Collectors.toList());
    }

    /* ======================= Setters ======================= */
    public void setId(long id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setScores() {
        this.scores = scores;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /* Method for TotalScore - Win - Lost - Tied */
    public long getWinScore() {
        return this.getScores()
                .stream()
                .filter(score -> score.getScore() == 1.0D)
                .count();
    }

    public double getTiedScore() {
        return this.getScores()
                .stream()
                .filter(score -> score.getScore() == 0.5D)
                .count();
    }

    public long getLostScore() {
        return this.getScores()
                .stream()
                .filter(score -> score.getScore() == 0D)
                .count();
    }

    public double getTotalScore() {
        return this.getWinScore() * 1.0D + this.getTiedScore() * 0.5D + this.getLostScore() * 0D;
    }

    /* Method */
    public void addGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayers.add(gamePlayer);
    }

    /* ======================= DTO ======================= */
    // DTO for Player where I have 'id' and 'username' for each player
    public Map<String, Object> getPlayerDTO() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", this.getId());
        dto.put("email", this.getUserName());
        return dto;
    }
}