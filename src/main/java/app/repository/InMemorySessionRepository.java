package app.repository;

import app.domain.Session;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final Map<Integer, Session> storage = new LinkedHashMap<>();

    @Override
    public synchronized void save(Session session) {
        storage.put(session.getId(), session);
    }

    @Override
    public synchronized Optional<Session> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Session> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public synchronized void deleteById(int id) {
        storage.remove(id);
    }
}
