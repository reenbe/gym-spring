package app.service;

import app.domain.GoldMember;
import app.domain.InvalidDataException;
import app.domain.Member;
import app.domain.MemberType;
import app.domain.NotFoundException;
import app.domain.RegularMember;
import app.domain.RuleViolationException;
import app.domain.Session;
import app.domain.SilverMember;
import app.domain.VipMember;
import app.repository.MemberRepository;
import app.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class GymService {

    private final MemberRepository members;
    private final SessionRepository sessions;
    private int nextMemberId = 1;
    private int nextSessionId = 1;

    public GymService(MemberRepository members, SessionRepository sessions) {
        this.members = members;
        this.sessions = sessions;
    }


    public synchronized Member addMember(MemberType type, String name, Integer age) {
        if (type == null) {
            throw new InvalidDataException("поле type обязательно: REGULAR, VIP, SILVER или GOLD");
        }
        if (age == null) {
            throw new InvalidDataException("поле age обязательно");
        }
        int id = nextMemberId;
        Member member = switch (type) {
            case REGULAR -> new RegularMember(name, age, id);
            case VIP -> new VipMember(name, age, id);
            case SILVER -> new SilverMember(name, age, id);
            case GOLD -> new GoldMember(name, age, id);
        };
        nextMemberId++;
        members.save(member);
        return member;
    }

    public synchronized Member getMember(int id) {
        return members.findById(id)
                .orElseThrow(() -> new NotFoundException("клиент с id=" + id + " не найден"));
    }

    public synchronized List<Member> listMembers(MemberType type, String namePart, Boolean canBook) {
        String part = namePart == null ? null : namePart.toLowerCase(Locale.ROOT);
        List<Member> result = new ArrayList<>();
        for (Member m : members.findAll()) {
            if (type != null && m.type() != type) continue;
            if (part != null && !m.getName().toLowerCase(Locale.ROOT).contains(part)) continue;
            if (canBook != null && m.canBook() != canBook) continue;
            result.add(m);
        }
        return result;
    }

    public synchronized Member updateMember(int id, MemberType type, String name, Integer age) {
        Member member = getMember(id);
        if (type != null && type != member.type()) {
            throw new InvalidDataException("тип абонемента изменить нельзя");
        }
        if (age == null) {
            throw new InvalidDataException("поле age обязательно");
        }
        member.update(name, age);
        return member;
    }

    public synchronized void deleteMember(int id) {
        getMember(id);
        for (Session s : sessions.findAll()) {
            if (s.isBooked(id)) {
                throw new RuleViolationException("нельзя удалить клиента, который записан на занятия");
            }
        }
        members.deleteById(id);
    }


    public synchronized Session addSession(String name, Integer capacity) {
        if (capacity == null) {
            throw new InvalidDataException("поле capacity обязательно");
        }
        int id = nextSessionId;
        Session session = new Session(id, name, capacity);
        nextSessionId++;
        sessions.save(session);
        return session;
    }

    public synchronized Session getSession(int id) {
        return sessions.findById(id)
                .orElseThrow(() -> new NotFoundException("занятие с id=" + id + " не найдено"));
    }

    public synchronized List<Session> listSessions(Boolean hasFreePlaces, String namePart) {
        String part = namePart == null ? null : namePart.toLowerCase(Locale.ROOT);
        List<Session> result = new ArrayList<>();
        for (Session s : sessions.findAll()) {
            if (hasFreePlaces != null && s.hasFreePlaces() != hasFreePlaces) continue;
            if (part != null && !s.getName().toLowerCase(Locale.ROOT).contains(part)) continue;
            result.add(s);
        }
        return result;
    }

    public synchronized Session updateSession(int id, String name, Integer capacity) {
        Session session = getSession(id);
        if (capacity == null) {
            throw new InvalidDataException("поле capacity обязательно");
        }
        session.update(name, capacity);
        return session;
    }

    public synchronized void deleteSession(int id) {
        Session session = getSession(id);
        if (!session.isEmpty()) {
            throw new RuleViolationException("нельзя удалить занятие, на которое кто-то записан");
        }
        sessions.deleteById(id);
    }


    public synchronized List<Member> membersOf(int sessionId) {
        Session session = getSession(sessionId);
        List<Member> result = new ArrayList<>();
        for (int memberId : session.getMemberIds()) {
            result.add(getMember(memberId));
        }
        return result;
    }

    public synchronized List<Session> sessionsOf(int memberId) {
        getMember(memberId);
        List<Session> result = new ArrayList<>();
        for (Session s : sessions.findAll()) {
            if (s.isBooked(memberId)) {
                result.add(s);
            }
        }
        return result;
    }


    public synchronized Session book(int sessionId, Integer memberId) {
        Session session = getSession(sessionId);
        if (memberId == null) {
            throw new InvalidDataException("поле memberId обязательно");
        }
        Member member = getMember(memberId);

        if (!member.isAdult()) {
            throw new RuleViolationException("записаться на занятие можно с 18 лет");
        }
        if (session.isBooked(member.getId())) {
            throw new RuleViolationException("клиент уже записан на это занятие");
        }
        if (!session.hasFreePlaces()) {
            throw new RuleViolationException("на занятии нет свободных мест");
        }
        if (!member.canBook()) {
            throw new RuleViolationException("лимит занятий по абонементу исчерпан");
        }

        session.addMember(member.getId());
        member.registerVisit();
        return session;
    }

    public synchronized Session cancel(int sessionId, Integer memberId) {
        Session session = getSession(sessionId);
        if (memberId == null) {
            throw new InvalidDataException("поле memberId обязательно");
        }
        Member member = getMember(memberId);

        if (!session.isBooked(member.getId())) {
            throw new RuleViolationException("клиент не записан на это занятие");
        }

        member.cancelVisit();
        session.removeMember(member.getId());
        return session;
    }
}
