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

    /* permite que los objetos sean compartidos y administrados por el framework */
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

    /* ======================= GAMES ======================= */
    // dto con info de Player y todos los Games
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
                .map(game -> makeGameDTO(game))
                .collect(toList()));

        return dto;
    }

    //Metodo que verifica si el usuario es Guest o User
    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    /* ======================= GAME VIEW ======================= */
    // dto del gamePlayer con id igual al id que me pasan, si esta autorizado
    @RequestMapping("/game_view/{gamePlayer_Id}")
    public ResponseEntity<Map<String, Object>> GameView(@PathVariable Long gamePlayer_Id,
                                                        Authentication authentication) {

        Player player = playerRepository.findByUserName(authentication.getName());
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayer_Id).orElse(null);

        if (player != null) {
            if (gamePlayer != null) {
                if (gamePlayer.getPlayer().getId() == player.getId()) {
                    return new ResponseEntity<>(makeGameViewDTO(gamePlayer), HttpStatus.CREATED);
                }
            }

            return new ResponseEntity<>(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
    }

    //Metodo para hacer DTO con key y value
    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /* ======================= GameViewDTO======================= */
    /* obtiene datos del game del gamePlayer, da info sobre ambos gp en ese game
     * con dto de ships del gp principal y sus salvoes */
    public Map<String, Object> makeGameViewDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<>();
        Map<String, Object> opdto = new LinkedHashMap<>();
        GamePlayer opponent = getOpponent(gamePlayer);

        if (opponent != null) {
            opdto.put("self", makeHitsDTO(gamePlayer));
            opdto.put("opponent", makeHitsDTO(opponent));
        } else {
            opdto.put("self", new ArrayList<>());
            opdto.put("opponent", new ArrayList<>());
        }

        Set<Ship> ships = gamePlayer.getShips();
        Set<Salvo> salvos = gamePlayer.getSalvos();
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gameState", getGameState(gamePlayer));
        dto.put("gamePlayers", getAllGamePlayers(gamePlayer.getGame().getGamePlayers()));
        dto.put("ships", getAllShips(ships));
        dto.put("salvoes", getAllSalvos(salvos));
        dto.put("hits", opdto);
        return dto;
    }

    /* ======================= Add Players ======================= */
    // Necesita username y password
    @RequestMapping(path = "/players", method = RequestMethod.POST)
    private ResponseEntity<Object> addPlayer(@RequestParam String email, @RequestParam String password) {

        if (email.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (playerRepository.findByUserName(email) != null) {
            return new ResponseEntity<>("Name already exists", HttpStatus.FORBIDDEN);
        }

        playerRepository.save(new Player(email, passwordEncoder.encode(password)));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    //Metodo para hacer DTO con key y value
    private Map<String, Object> MakeMap(String key, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /* ======================= Join Game ======================= */
    /*  Metodo que permite unirse a la partida ingresada por parametro
     * verifica player
     * "Join game" button en el front end */
    @RequestMapping(path = "/game/{gameid}/players", method = RequestMethod.POST)
    private ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameid,
                                                         Authentication authentication) {

        if (authentication == null) {
            return new ResponseEntity<>(MakeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Game joinGame = gameRepository.getOne(gameid);
        if (joinGame == null) {
            return new ResponseEntity<>(MakeMap("error", "No such game"), HttpStatus.FORBIDDEN);
        }
        if (joinGame.getGamePlayers().size() >= 2) {
            return new ResponseEntity<>(MakeMap("error", "Game is full"), HttpStatus.FORBIDDEN);
        }else {
            if (joinGame.getAllUserNames().contains(authentication.getName())){
                return new ResponseEntity<>(makeMap("Error", "You can't play with yourself"), HttpStatus.FORBIDDEN);
            }
        }

        GamePlayer gamePlayer = gamePlayerRepository.save(new GamePlayer(joinGame, playerRepository.findByUserName(authentication.getName())));
        return new ResponseEntity<>(makeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
    }

    /* ======================= Add Ships ======================= */
    /* Metodo que devuelve los ships del player pasado por parametro en la url
     *  verifica player
     * "add ships" button en el front end */
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
    /* verifica player
     * "add salvos" button en el front end. */
    @RequestMapping(path = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.POST)
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
        int turn = gamePlayer.getSalvos().size() + 1;
        if (salvo.getTurn() > turn || gamePlayer.getSalvos().size() > getOpponent(gamePlayer).getSalvos().size()) {
            return new ResponseEntity<>(makeMap("Error", "You can't fire salvos"), HttpStatus.FORBIDDEN);
        } else {
            if (salvo.getSalvoLocations().size() > 5) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                Salvo salvo1 = new Salvo(turn, gamePlayer, salvo.getSalvoLocations());
                salvoRepository.save(salvo1);
                return new ResponseEntity<>(makeMap("Ok", "Salvo added"), HttpStatus.CREATED);
            }
        }
    }

    // Tabla de clasificasiones
    @RequestMapping("/leaderBoard")
    private List<Map<String, Object>> getLeaderBoard() {
        return playerRepository.findAll()
                .stream()
                .map(player -> playerLeaderBoardDTO(player))
                .collect(Collectors.toList());
    }

    /* ======================= GamePlayer DTOs ======================= */
    // Lista de dto de cada ship para cada gamePlayer
    private List<Map<String, Object>> getAllShips(Set<Ship> ships) {
        return ships
                .stream()
                .map(ship -> ship.shipDTO())
                .collect(Collectors.toList());
    }

    // Salvo Locations
    private List<Map<String, Object>> getAllSalvos(Set<Salvo> salvos) {
        return salvos
                .stream()
                .map(salvo -> salvo.salvoDTO())
                .collect(Collectors.toList());
    }

    // Score
    private List<Map<String, Object>> getAllScore(Set<Score> scores) {
        return scores
                .stream()
                .map(score -> score.scoreDTO())
                .collect(Collectors.toList());
    }

    private Map<String, Object> getPlayerScoreDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("total", player.getTotalScore());
        dto.put("won", player.getWinScore());
        dto.put("lost", player.getLostScore());
        dto.put("tied", player.getTiedScore());
        return dto;
    }

    // LeaderBoard
    private Map<String, Object> playerLeaderBoardDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        dto.put("score", this.getPlayerScoreDTO(player));
        return dto;
    }

    // Hits dto: para cada turno qué ships han sido golpeados y/o hundidos
    public List<Map<String, Object>> makeHitsDTO(GamePlayer gamePlayer) {
        List<Map<String, Object>> list = new ArrayList<>();
        long
                //acum para los danos
                carrierDamage = 0,
                battleshipDamage = 0,
                submarineDamage = 0,
                destroyerDamage = 0,
                patrolboatDamage = 0;

        for (Salvo salvo : getOpponent(gamePlayer).getSalvos()) {
            long
                    carrieHits = getHitsType(gamePlayer, salvo, "carrier"),
                    battleshipHits = getHitsType(gamePlayer, salvo, "battleship"),
                    submarineHits = getHitsType(gamePlayer, salvo, "submarine"),
                    destroyerHits = getHitsType(gamePlayer, salvo, "destroyer"),
                    patrolboatHits = getHitsType(gamePlayer, salvo, "patrolboat");


            Map<String, Object> dto = new LinkedHashMap<String, Object>();
            Map<String, Object> damageDTO = new LinkedHashMap<>();

            dto.put("turn", salvo.getTurn());
            dto.put("hitLocations", getHitsLocations(gamePlayer, salvo));
            dto.put("damages", damageDTO);
            dto.put("missed", salvo.getSalvoLocations().size() - getHitsLocations(gamePlayer, salvo).size());

            damageDTO.put("carrierHits", carrieHits);
            damageDTO.put("battleshipHits", battleshipHits);
            damageDTO.put("submarineHits", submarineHits);
            damageDTO.put("destroyerHits", destroyerHits);
            damageDTO.put("patrolboatHits", patrolboatHits);

            carrierDamage += carrieHits;
            battleshipDamage += battleshipHits;
            submarineDamage += submarineHits;
            destroyerDamage += destroyerHits;
            patrolboatDamage += patrolboatHits;

            damageDTO.put("carrier", carrierDamage);
            damageDTO.put("battleship", battleshipDamage);
            damageDTO.put("submarine", submarineDamage);
            damageDTO.put("destroyer", destroyerDamage);
            damageDTO.put("patrolboat", patrolboatDamage);

            list.add(dto);
        }

        return list;
    }

    /*  ======================= Game DTOs ======================= */
    // Game dto
    public Map<String, Object> makeGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("created", game.getCreationDate());
        dto.put("gamePlayers", getAllGamePlayers(game.getGamePlayers()));
        dto.put("scores", getAllScore(game.getScores()));
        return dto;
    }

    // Lista de dtos de todos los gamePlayers
    private List<Map<String, Object>> getAllGamePlayers(Set<GamePlayer> gamePlayers) {
        return gamePlayers
                .stream()
                .map(gamePlayer -> gamePlayer.getGamePlayerDTO())
                .collect(Collectors.toList());
    }

    /* ======================= Game State ======================= */
    public String getGameState(GamePlayer gamePlayer) {

        if (gamePlayer.getShips().size() == 0) {
            return "PLACESHIPS";
        }

        if (getOpponent(gamePlayer) != null) {
            if (getOpponent(gamePlayer).getShips().size() == 0) {
                return "WAITINGFOROPP";
            }

            // si mis salvoes = a los del opp y los ships sunk mios y del opp < 17: PLAY
            if ((gamePlayer.getSalvos().size() == getOpponent(gamePlayer).getSalvos().size()) &&
                    (getSunks(getOpponent(gamePlayer)) < 17 && getSunks(gamePlayer) < 17)) {
                return "PLAY";
            }

            if ((gamePlayer.getSalvos().size() > getOpponent(gamePlayer).getSalvos().size())) {
                return "WAIT";
            }

            // sunks ships mios < 17 y los sunks ships del opp == 17: WON genero score
            Date date = new Date();
            if (getSunks(gamePlayer) < 17 && getSunks(getOpponent(gamePlayer)) == 17) {
                if (gamePlayer.getGame().getScores().isEmpty()) {
                    Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 1, date);
                    scoreRepository.save(newScore);
                }
                return "WON";

                // si no: mis ships sunks == 17 y los del opp < 17: LOST
            } else if (getSunks(gamePlayer) == 17 && getSunks(getOpponent(gamePlayer)) < 17) {
                if (gamePlayer.getGame().getScores().isEmpty()) {
                    Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 0, date);
                    scoreRepository.save(newScore);
                }
                return "LOST";

                // si no: mis ships sunk = 17 y los del opp = 17: TIE
            } else if (getSunks(gamePlayer) == 17 && getSunks(getOpponent(gamePlayer)) == 17) {
                if (gamePlayer.getGame().getScores().isEmpty()) {
                    Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 0.5, date);
                    scoreRepository.save(newScore);
                }
                return "TIE";

            } else
                return "WAIT";
        }else {
           return "WAITINGFOROPP"; }
    }
 /*     refactorizar: WAIT: asi aparecen los dos como won

            Date date = new Date();
            if (!(gamePlayer.getSalvos().size() > getOpponent(gamePlayer).getSalvos().size())) {
                if (getSunks(gamePlayer) < 17 && getSunks(getOpponent(gamePlayer)) == 17) {
                    if (gamePlayer.getGame().getScores().isEmpty()) {
                    Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 1, date);
                    scoreRepository.save(newScore);
                }
            }
            return "WON";

        } else if (getSunks(gamePlayer) == 17 && getSunks(getOpponent(gamePlayer)) < 17) {
            if (gamePlayer.getGame().getScores().isEmpty()) {
                Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 0, date);
                scoreRepository.save(newScore);
            }
            return "LOST";

        } else if (getSunks(gamePlayer) == 17 && getSunks(getOpponent(gamePlayer)) == 17) {
            if (gamePlayer.getGame().getScores().isEmpty()) {
                Score newScore = new Score(gamePlayer.getGame(), gamePlayer.getPlayer(), 0.5, date);
                scoreRepository.save(newScore);
            }
            return "TIE";

        } else
            return "WAIT";
    }*/

    // indica numero de sunks ships para verificar game over
    private int getSunks(GamePlayer gamePlayer) {
        GamePlayer opp = getOpponent(gamePlayer);
        List<String> ships = new ArrayList<>();
        List<String> salvos = new ArrayList<>();
        for (Ship ship : gamePlayer.getShips()) {
            ships.addAll(ship.getLocations());
        }
        for (Salvo salvo : opp.getSalvos()) {
            salvos.addAll(salvo.getSalvoLocations());
        }
        ships.retainAll(salvos);
        return ships.size();
    }

    // Get Opponent
    private GamePlayer getOpponent(GamePlayer gamePlayer) {
        GamePlayer opponent = null;
        for (GamePlayer gamePlayer1 : gamePlayer.getGame().getGamePlayers()) {
            if (gamePlayer1.getId() != gamePlayer.getId()) {
                opponent = gamePlayer1;
            }
        }
        return opponent;
    }

    // get Hits Type
    private long getHitsType(GamePlayer gamePlayer, Salvo salvo, String type) {
        List<String> ships = gamePlayer.getShips()
                .stream()
                .filter(ship -> ship.getType().equals(type))
                .flatMap(ship -> ship.getLocations().stream())
                .collect(toList());
        ships.retainAll(salvo.getSalvoLocations());

        if (ships.size() == 0) {
            return 0;
        }
        return ships.size();
    }

    // get Hits Locations
    private List<String> getHitsLocations(GamePlayer gamePlayer, Salvo salvo) {
        List<String> ships = gamePlayer.getShips()
                .stream()
                .flatMap(ship -> ship.getLocations().stream())
                .collect(toList());
        ships.retainAll(salvo.getSalvoLocations());
        return ships;
    }
}
