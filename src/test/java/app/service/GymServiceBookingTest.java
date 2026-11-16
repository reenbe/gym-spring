package app.service;

import app.domain.InvalidDataException;
import app.domain.Member;
import app.domain.MemberType;
import app.domain.NotFoundException;
import app.domain.RuleViolationException;
import app.domain.Session;
import app.repository.InMemoryMemberRepository;
import app.repository.InMemorySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Запись на занятие и отмена: главная операция, которая меняет состояние по правилам. */
class GymServiceBookingTest {

    private GymService service;

    @BeforeEach
    void setUp() {
        service = new GymService(new InMemoryMemberRepository(), new InMemorySessionRepository());
    }

    private static List<String> names(List<Member> members) {
        return members.stream().map(Member::getName).toList();
    }

    // ---------- запись: успех и изменение состояния ----------

    @Test
    @DisplayName("Запись добавляет клиента и уменьшает число свободных мест")
    void bookAddsMemberAndTakesAPlace() {
        Session session = service.addSession("Yoga", 2);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        Session result = service.book(session.getId(), member.getId());

        assertTrue(result.isBooked(member.getId()));
        assertEquals(1, result.getFreePlaces());
        assertEquals(List.of(member.getId()), service.getSession(session.getId()).getMemberIds());
    }

    @Test
    @DisplayName("VIP: запись увеличивает счётчик посещений, REGULAR: не увеличивает")
    void bookCountsVisitsOnlyForCountedTypes() {
        Session session = service.addSession("Yoga", 5);
        Member vip = service.addMember(MemberType.VIP, "Anna", 30);
        Member regular = service.addMember(MemberType.REGULAR, "Ivan", 25);

        service.book(session.getId(), vip.getId());
        service.book(session.getId(), regular.getId());

        assertEquals(1, vip.getVisits());
        assertEquals(0, regular.getVisits());
    }

    @Test
    @DisplayName("Граница вместимости: последнее место занять можно, следующий клиент получает отказ")
    void bookLastPlaceThenFull() {
        Session session = service.addSession("Yoga", 2);
        Member a = service.addMember(MemberType.REGULAR, "A", 20);
        Member b = service.addMember(MemberType.REGULAR, "B", 21);
        Member c = service.addMember(MemberType.VIP, "C", 22);

        service.book(session.getId(), a.getId());
        service.book(session.getId(), b.getId());

        assertThrows(RuleViolationException.class, () -> service.book(session.getId(), c.getId()));
        assertFalse(service.getSession(session.getId()).isBooked(c.getId()));
        assertEquals(0, c.getVisits());
        assertEquals(0, service.getSession(session.getId()).getFreePlaces());
    }

    // ---------- запись: ошибки ----------

    @Test
    @DisplayName("Повторная запись запрещена и не увеличивает счётчик посещений")
    void cannotBookTwice() {
        Session session = service.addSession("Yoga", 5);
        Member vip = service.addMember(MemberType.VIP, "Anna", 30);
        service.book(session.getId(), vip.getId());

        assertThrows(RuleViolationException.class, () -> service.book(session.getId(), vip.getId()));

        assertEquals(1, vip.getVisits());
        assertEquals(4, service.getSession(session.getId()).getFreePlaces());
    }

    @Test
    @DisplayName("Без memberId записать нельзя")
    void bookRequiresMemberId() {
        Session session = service.addSession("Yoga", 5);

        assertThrows(InvalidDataException.class, () -> service.book(session.getId(), null));
    }

    @Test
    @DisplayName("Несуществующее занятие или клиент дают NotFound")
    void bookUnknownResources() {
        Session session = service.addSession("Yoga", 5);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        assertThrows(NotFoundException.class, () -> service.book(999, member.getId()));
        assertThrows(NotFoundException.class, () -> service.book(session.getId(), 999));
    }

    // ---------- лимиты абонементов ----------

    @Test
    @DisplayName("SILVER: восьмая запись проходит, девятая отклоняется и ничего не меняет")
    void silverLimitStopsNinthBooking() {
        Member silver = service.addMember(MemberType.SILVER, "Limit", 40);
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            sessions.add(service.addSession("S" + i, 10));
        }
        for (int i = 0; i < 8; i++) {
            service.book(sessions.get(i).getId(), silver.getId());
        }
        Session ninth = sessions.get(8);

        assertThrows(RuleViolationException.class, () -> service.book(ninth.getId(), silver.getId()));

        assertEquals(8, silver.getVisits());
        assertFalse(silver.canBook());
        assertFalse(service.getSession(ninth.getId()).isBooked(silver.getId()));
    }

    @Test
    @DisplayName("SILVER: после отмены одной записи можно записаться снова")
    void silverCanBookAgainAfterCancel() {
        Member silver = service.addMember(MemberType.SILVER, "Limit", 40);
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            sessions.add(service.addSession("S" + i, 10));
        }
        for (int i = 0; i < 8; i++) {
            service.book(sessions.get(i).getId(), silver.getId());
        }

        service.cancel(sessions.get(0).getId(), silver.getId());
        service.book(sessions.get(8).getId(), silver.getId());

        assertEquals(8, silver.getVisits());
        assertTrue(service.getSession(sessions.get(8).getId()).isBooked(silver.getId()));
        assertFalse(service.getSession(sessions.get(0).getId()).isBooked(silver.getId()));
    }

    @Test
    @DisplayName("GOLD: лимит 12, тринадцатая запись отклоняется")
    void goldLimitStopsThirteenthBooking() {
        Member gold = service.addMember(MemberType.GOLD, "Gold", 35);
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            sessions.add(service.addSession("G" + i, 10));
        }
        for (int i = 0; i < 12; i++) {
            service.book(sessions.get(i).getId(), gold.getId());
        }

        assertThrows(RuleViolationException.class, () -> service.book(sessions.get(12).getId(), gold.getId()));
        assertEquals(12, gold.getVisits());
    }

    // ---------- отмена ----------

    @Test
    @DisplayName("Отмена освобождает место и уменьшает счётчик посещений")
    void cancelFreesPlaceAndVisit() {
        Session session = service.addSession("Yoga", 1);
        Member vip = service.addMember(MemberType.VIP, "Anna", 30);
        service.book(session.getId(), vip.getId());

        Session result = service.cancel(session.getId(), vip.getId());

        assertFalse(result.isBooked(vip.getId()));
        assertEquals(1, result.getFreePlaces());
        assertEquals(0, vip.getVisits());
    }

    @Test
    @DisplayName("После отмены на освободившееся место записывается другой клиент")
    void cancelThenAnotherMemberBooks() {
        Session session = service.addSession("Yoga", 1);
        Member first = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member second = service.addMember(MemberType.REGULAR, "Anna", 26);
        service.book(session.getId(), first.getId());

        service.cancel(session.getId(), first.getId());
        service.book(session.getId(), second.getId());

        assertEquals(List.of(second.getId()), service.getSession(session.getId()).getMemberIds());
    }

    @Test
    @DisplayName("Отменить запись, которой нет, нельзя")
    void cannotCancelNotBooked() {
        Session session = service.addSession("Yoga", 5);
        Member member = service.addMember(MemberType.VIP, "Anna", 30);

        assertThrows(RuleViolationException.class, () -> service.cancel(session.getId(), member.getId()));

        assertEquals(0, member.getVisits());
    }

    @Test
    @DisplayName("Отмена без memberId или с несуществующими ресурсами даёт ошибку")
    void cancelValidatesInput() {
        Session session = service.addSession("Yoga", 5);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        assertThrows(InvalidDataException.class, () -> service.cancel(session.getId(), null));
        assertThrows(NotFoundException.class, () -> service.cancel(999, member.getId()));
        assertThrows(NotFoundException.class, () -> service.cancel(session.getId(), 999));
    }

    // ---------- связи ----------

    @Test
    @DisplayName("membersOf возвращает записанных клиентов, sessionsOf возвращает занятия клиента")
    void relationsReflectBookings() {
        Session yoga = service.addSession("Yoga", 5);
        Session pilates = service.addSession("Pilates", 5);
        Member ivan = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member anna = service.addMember(MemberType.VIP, "Anna", 30);
        service.book(yoga.getId(), ivan.getId());
        service.book(yoga.getId(), anna.getId());
        service.book(pilates.getId(), ivan.getId());

        assertEquals(List.of("Ivan", "Anna"), names(service.membersOf(yoga.getId())));
        assertEquals(List.of("Ivan"), names(service.membersOf(pilates.getId())));
        assertEquals(2, service.sessionsOf(ivan.getId()).size());
        assertEquals(1, service.sessionsOf(anna.getId()).size());
    }

    @Test
    @DisplayName("После отмены связи обновляются")
    void relationsUpdateAfterCancel() {
        Session yoga = service.addSession("Yoga", 5);
        Member ivan = service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.book(yoga.getId(), ivan.getId());

        service.cancel(yoga.getId(), ivan.getId());

        assertTrue(service.membersOf(yoga.getId()).isEmpty());
        assertTrue(service.sessionsOf(ivan.getId()).isEmpty());
    }

    @Test
    @DisplayName("Связи несуществующих ресурсов дают NotFound")
    void relationsOfUnknownResources() {
        assertThrows(NotFoundException.class, () -> service.membersOf(999));
        assertThrows(NotFoundException.class, () -> service.sessionsOf(999));
    }
}
