package com.codeoftheweb.salvo.controller;

import com.codeoftheweb.salvo.models.Game;
import com.codeoftheweb.salvo.models.GamePlayer;
import com.codeoftheweb.salvo.repositories.GamePlayerRepository;
import com.codeoftheweb.salvo.repositories.GameRepository;
import com.codeoftheweb.salvo.repositories.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api") //para cambiar la raiz de la ruta
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /* ======================= Create Game ======================= */
    /*metodo que verifica si el usuario esta autenticado y crea un nuevo juego*/

    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(MakeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Game game = gameRepository.save(new Game());
        GamePlayer gamePlayer = gamePlayerRepository.save(new GamePlayer(game, playerRepository.findByUserName(authentication.getName())));
        return new ResponseEntity<>(MakeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
    }

    private Map<String, Object> MakeMap(String error, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(error, value);
        return map;
    }

}
