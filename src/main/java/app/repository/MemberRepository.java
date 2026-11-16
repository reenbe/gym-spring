package app.repository;

import app.domain.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    void save(Member member);

    Optional<Member> findById(int id);

    List<Member> findAll();

    void deleteById(int id);
}
