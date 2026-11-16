package app.repository;

import app.domain.Session;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    void save(Session session);

    Optional<Session> findById(int id);

    List<Session> findAll();

    void deleteById(int id);
}
