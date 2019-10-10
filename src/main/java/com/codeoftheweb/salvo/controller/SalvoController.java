package com.codeoftheweb.salvo.controller;

import com.codeoftheweb.salvo.models.*;
import com.codeoftheweb.salvo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api") //para cambiar la raiz de la ruta
public class SalvoController {

    /*permite que los objetos sean compartidos y administrados por el framework*/
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private SalvoRepository salvoRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /* ======================= API GAMES ======================= */
    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();

        if (isGuest(authentication)) {
            dto.put("player", "Guest");
        } else {
            Player player = playerRepository.findByUserName(authentication.getName());
            dto.put("player", player.getPlayerDTO());
        }

        dto.put("games", gameRepository.findAll()
                .stream()
                .map(game -> getGameDTO(game))
                .collect(toList()));

        return dto;
    }

    /* ======================= GAME VIEW ======================= */
    @RequestMapping("/game_view/{gamePlayer_Id}")
    public ResponseEntity<Map<String, Object>> getGameView(@PathVariable Long gamePlayer_Id,
                                                           Authentication authentication) {
        Player player = playerRepository.findByUserName(authentication.getName());
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayer_Id).get();
        if (player != null) {
            if (gamePlayer.getPlayer().getId() == player.getId()) {
                return new ResponseEntity<>(getGameViewDTO(gamePlayer), HttpStatus.CREATED);
            }
        }

        return new ResponseEntity<>(makeMap("error", "Las credenciales no coinciden"), HttpStatus.UNAUTHORIZED);
    }

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /* ======================= GameViewDTO======================= */
    public Map<String, Object> getGameViewDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<>();
        Map<String, Object> opdto = new LinkedHashMap<>();
        GamePlayer opponent = getOpponent(gamePlayer);

        if (opponent != null) {
            opdto.put("self", getHitsDTO(gamePlayer));
            opdto.put("opponent", getHitsDTO(opponent));
        } else {
            opdto.put("self", new ArrayList<>());
            opdto.put("opponent", new ArrayList<>());
        }

        Set<Ship> ships = gamePlayer.getShips();
        Set<Salvo> salvos = gamePlayer.getSalvos();
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gameState", "PLACESHIPS");
        dto.put("gamePlayers", getAllGamePlayers(gamePlayer.getGame().getGamePlayers()));
        dto.put("ships", getAllShips(ships));
        dto.put("salvoes", getAllSalvos(salvos));
        dto.put("hits", opdto);
        return dto;
    }


    /* ======================= Opponent ======================= */
    private GamePlayer getOpponent(GamePlayer gamePlayer) {
        GamePlayer opponent = null;
        for (GamePlayer gamePlayer1 : gamePlayer.getGame().getGamePlayers()) {
            if (gamePlayer1.getId() != gamePlayer.getId()) {
                opponent = gamePlayer1;
            }
        }
        return opponent;
    }

    /* ======================= Hits Dto ======================= */
    public List<Map<String, Object>> getHitsDTO(GamePlayer gamePlayer) {
        List<Map<String, Object>> list = new ArrayList<>();
        long destroyerDemage = 0, carrierDemage = 0, battleshipDemage = 0, submarineDemage = 0, patrolboatDemage = 0;

        for (Salvo salvo : getOpponent(gamePlayer).getSalvos()) {
            long carrieHits = 0, battleshipHits = 0, submarineHits = 0, destroyerHits = 0, patrolboatHits = 0;


            Map<String, Object> dto = new LinkedHashMap<String, Object>();
            Map<String, Object> demageDto = new LinkedHashMap<>();

            dto.put("turn", salvo.getTurn());
            dto.put("hitLocations", getHitsLocations(gamePlayer, salvo));
            dto.put("damages", demageDto);
            dto.put("missed", "");

            demageDto.put("carrierHits", getHitsType(gamePlayer, salvo, "Carrier"));
            demageDto.put("battleshipHits", getHitsType(gamePlayer, salvo, "Cattleship"));
            demageDto.put("submarineHits", getHitsType(gamePlayer, salvo, "Submarine"));
            demageDto.put("destroyerHits", getHitsType(gamePlayer, salvo, "Destroyer"));
            demageDto.put("patrolboatHits", getHitsType(gamePlayer, salvo, "Patrol Boat"));

            demageDto.put("carrier", carrierDemage + getHitsType(gamePlayer, salvo, "Carrier"));
            demageDto.put("battleship", battleshipDemage + getHitsType(gamePlayer, salvo, "Battleship"));
            demageDto.put("submarine", submarineDemage + getHitsType(gamePlayer, salvo, "Submarine"));
            demageDto.put("destroyer", destroyerDemage + getHitsType(gamePlayer, salvo, "Destroyer"));
            demageDto.put("patrolboat", patrolboatDemage + getHitsType(gamePlayer, salvo, "Patrol Boat"));


            list.add(dto);
        }

        return list;
    }


    /* ======================= Hits ======================= */

    private long getHitsType(GamePlayer gamePlayer, Salvo salvo, String type) {
        List<String> ships = gamePlayer.getShips().stream().filter(ship -> ship.getType() == type)
                .flatMap(ship -> ship.getLocations().stream()).collect(toList());
        ships.retainAll(salvo.getSalvoLocations());
        if (ships.size() == 0) {
            return 0;

        }
        return ships.size();
    }

    private List<String> getHitsLocations(GamePlayer gamePlayer, Salvo salvo) {
        List<String> ships = gamePlayer.getShips().stream()
                .flatMap(ship -> ship.getLocations().stream()).collect(toList());
        ships.retainAll(salvo.getSalvoLocations());
        return ships;
    }

    /* ======================= GamePlayer ======================= */
    private List<Map<String, Object>> getAllGamePlayers(Set<GamePlayer> gamePlayers) {
        return gamePlayers
                .stream()
                .map(gamePlayer -> gamePlayer.getGamePlayerDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Ship Location ======================= */
    private List<Map<String, Object>> getAllShips(Set<Ship> ships) {
        return ships
                .stream()
                .map(ship -> ship.shipDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Salvo Location ======================= */
    private List<Map<String, Object>> getAllSalvos(Set<Salvo> salvos) {
        return salvos
                .stream()
                .map(salvo -> salvo.salvoDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Score ======================= */
    @RequestMapping("/leaderBoard")
    private List<Map<String, Object>> getLeaderBoard() {
        return playerRepository.findAll()
                .stream()
                .map(player -> playerLeaderBoardDto(player))
                .collect(Collectors.toList());
    }

    private Map<String, Object> playerLeaderBoardDto(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        dto.put("score", this.getPlayerScoreDTO(player));
        return dto;
    }

    private Map<String, Object> getPlayerScoreDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("total", player.getTotalScore());
        dto.put("won", player.getWinScore());
        dto.put("lost", player.getLostScore());
        dto.put("tied", player.getTiedScore());
        return dto;
    }

    private List<Map<String, Object>> getAllScore(Set<Score> scores) {
        return scores
                .stream()
                .map(score -> score.scoreDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Agregar Players ======================= */
    @RequestMapping(path = "/players", method = RequestMethod.POST)
    private ResponseEntity<Object> addPlayer(
            @RequestParam String email, @RequestParam String password) {

        if (email.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (playerRepository.findByUserName(email) != null) {
            return new ResponseEntity<>("Name already exists", HttpStatus.FORBIDDEN);
        }

        playerRepository.save(new Player(email, passwordEncoder.encode(password)));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    private Map<String, Object> MakeMap(String error, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put(error, value);
        return map;
    }

    /* ======================= Join and create game ======================= */
    /*Metodo que permite unirse a la partida ingresada por parametro*/
    @RequestMapping(path = "/game/{id}/players", method = RequestMethod.POST)
    private ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long id,
                                                         Authentication authentication) {

        if (authentication == null) {
            return new ResponseEntity<>(MakeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Game joinGame = gameRepository.getOne(id);
        if (joinGame == null) {
            return new ResponseEntity<>(MakeMap("error", "No such game"), HttpStatus.FORBIDDEN);
        }
        if (joinGame.getGamePlayers().size() >= 2) {
            return new ResponseEntity<>(MakeMap("error", "Game is full"), HttpStatus.FORBIDDEN);
        }

        GamePlayer gamePlayer = gamePlayerRepository.save(new GamePlayer(joinGame, playerRepository.findByUserName(authentication.getName())));
        return new ResponseEntity<>(makeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
    }

    /* ======================= Add Ships ======================= */
    /*Metodo que devuelve los ships del player pasado por parametro en la url*/
    @RequestMapping(path = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addShips(@PathVariable Long gamePlayerId,
                                                        @RequestBody Set<Ship> ships,
                                                        Authentication authentication) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).get();
        Player player = playerRepository.findByUserName(authentication.getName());

        if (isGuest(authentication)) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no current user logged in"), HttpStatus.UNAUTHORIZED);
        }

        if (!gamePlayerRepository.findById(gamePlayerId).isPresent()) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no game player with the given ID"), HttpStatus.UNAUTHORIZED);
        }

        if (gamePlayer.getPlayer().getId() != player.getId()) {
            return new ResponseEntity<>(makeMap("error", "The current player is not the game player the ID references"),
                    HttpStatus.UNAUTHORIZED);
        }

        if (!gamePlayer.getShips().isEmpty()) {
            return new ResponseEntity<>
                    (makeMap("error", "The player already has ships placed"), HttpStatus.FORBIDDEN);
        }

        ships.forEach(ship -> ship.setGamePlayer(gamePlayer));
        shipRepository.saveAll(ships);
        return new ResponseEntity<>(makeMap("addShips", "Ships created"), HttpStatus.CREATED);
    }

    /* ======================= Add Salvos ======================= */
    @RequestMapping(path = "/games/players/{gamePlayerId}/salvos", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addSalvoes(@PathVariable Long gamePlayerId,
                                                          @RequestBody Salvo salvo,
                                                          Authentication authentication) {

        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).get();
        Player player = playerRepository.findByUserName(authentication.getName());

        if (isGuest(authentication)) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no current user logged in"), HttpStatus.UNAUTHORIZED);
        }

        if (!gamePlayerRepository.findById(gamePlayerId).isPresent()) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no game player with the given ID"), HttpStatus.UNAUTHORIZED);
        }

        if (gamePlayer.getPlayer().getId() != player.getId()) {
            return new ResponseEntity<>(makeMap("error", "The current player is not the game player the ID references"),
                    HttpStatus.UNAUTHORIZED);
        }

        Set<Salvo> salvos = gamePlayer.getSalvos();
        for (Salvo salvo1 : salvos) {
            if (salvo.getTurn() == salvo1.getTurn()) {
                return new ResponseEntity<>
                        (makeMap("error", "The player already has submitted a salvo for the turn listed"),
                                HttpStatus.FORBIDDEN);
            }
        }

        salvo.setGamePlayer(gamePlayer);
        salvoRepository.save(salvo);
        return new ResponseEntity<>(makeMap("addSalvoes", "Salvos save"), HttpStatus.CREATED);
    }

    /* ======================= Metodos =======================*/
    //Metodo que verifica si el usuario es Guest o User
    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    private Player getAuthentication(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        } else {
            return (playerRepository.findByUserName(authentication.getName()));
        }
    }

    /* ======================= GameDTO ======================= */
    public Map<String, Object> getGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("created", game.getCreationDate());
        dto.put("gamePlayers", getAllGamePlayers(game.getGamePlayers()));
        dto.put("scores", getAllScore(game.getScores()));
        return dto;
    }
}



