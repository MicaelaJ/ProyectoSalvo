package com.codeoftheweb.salvo.repositories;

import com.codeoftheweb.salvo.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface GameRepository extends JpaRepository<Game, Long> {
    @Override
    Game getOne(Long id);

    List<Game> findByCreationDate(Date creationDate);
}
//no es necesario el finby...
