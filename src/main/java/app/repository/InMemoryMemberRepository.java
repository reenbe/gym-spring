package app.repository;

import app.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<Integer, Member> storage = new LinkedHashMap<>();

    @Override
    public synchronized void save(Member member) {
        storage.put(member.getId(), member);
    }

    @Override
    public synchronized Optional<Member> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Member> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public synchronized void deleteById(int id) {
        storage.remove(id);
    }
}
