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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Занятия: создание, чтение, фильтры, изменение, удаление. */
class GymServiceSessionTest {

    private GymService service;

    @BeforeEach
    void setUp() {
        service = new GymService(new InMemoryMemberRepository(), new InMemorySessionRepository());
    }

    private static List<String> names(List<Session> sessions) {
        return sessions.stream().map(Session::getName).toList();
    }

    // ---------- создание ----------

    @Test
    @DisplayName("Занятиям выдаются последовательные id, свободны все места")
    void addSessionAssignsIds() {
        Session first = service.addSession("Yoga", 10);
        Session second = service.addSession("Pilates", 5);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(10, first.getFreePlaces());
    }

    @Test
    @DisplayName("Без вместимости занятие не создать")
    void addSessionRequiresCapacity() {
        assertThrows(InvalidDataException.class, () -> service.addSession("Yoga", null));
    }

    @Test
    @DisplayName("Границы вместимости: 1 и 500 можно, 0 и 501 нельзя")
    void capacityBoundaries() {
        assertThrows(InvalidDataException.class, () -> service.addSession("Box", 0));
        assertThrows(InvalidDataException.class, () -> service.addSession("Box", 501));

        assertEquals(1, service.addSession("Min", 1).getCapacity());
        assertEquals(500, service.addSession("Max", 500).getCapacity());
    }

    @Test
    @DisplayName("Пустое название даёт ошибку и не занимает id")
    void failedCreationDoesNotConsumeId() {
        assertThrows(InvalidDataException.class, () -> service.addSession("  ", 5));

        Session session = service.addSession("Yoga", 5);

        assertEquals(1, session.getId());
    }

    // ---------- чтение ----------

    @Test
    @DisplayName("getSession возвращает созданное занятие")
    void getSessionReturnsStoredSession() {
        Session created = service.addSession("Yoga", 5);

        assertSame(created, service.getSession(created.getId()));
    }

    @Test
    @DisplayName("getSession несуществующего занятия даёт NotFound")
    void getUnknownSessionFails() {
        assertThrows(NotFoundException.class, () -> service.getSession(999));
    }

    // ---------- список и фильтры ----------

    @Test
    @DisplayName("Без фильтров приходят все занятия в порядке создания")
    void listWithoutFilters() {
        service.addSession("Yoga", 5);
        service.addSession("Pilates", 5);

        assertEquals(List.of("Yoga", "Pilates"), names(service.listSessions(null, null)));
    }

    @Test
    @DisplayName("Фильтр hasFreePlaces разделяет заполненные и свободные занятия")
    void filterByFreePlaces() {
        Session full = service.addSession("Full", 1);
        service.addSession("Free", 5);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.book(full.getId(), member.getId());

        assertEquals(List.of("Free"), names(service.listSessions(true, null)));
        assertEquals(List.of("Full"), names(service.listSessions(false, null)));
    }

    @Test
    @DisplayName("Фильтр по названию не зависит от регистра и комбинируется с hasFreePlaces")
    void filterByNameAndCombined() {
        service.addSession("Yoga morning", 5);
        service.addSession("Yoga evening", 5);
        service.addSession("Pilates", 5);

        assertEquals(List.of("Yoga morning", "Yoga evening"), names(service.listSessions(null, "YOGA")));
        assertEquals(List.of("Pilates"), names(service.listSessions(true, "pil")));
        assertTrue(service.listSessions(null, "zzz").isEmpty());
    }

    // ---------- изменение ----------

    @Test
    @DisplayName("updateSession меняет название и вместимость")
    void updateChangesNameAndCapacity() {
        Session session = service.addSession("Yoga", 5);

        Session updated = service.updateSession(session.getId(), "Yoga 2", 8);

        assertEquals("Yoga 2", updated.getName());
        assertEquals(8, updated.getCapacity());
        assertEquals(8, service.getSession(session.getId()).getFreePlaces());
    }

    @Test
    @DisplayName("Нельзя уменьшить вместимость ниже числа записанных, занятие не меняется")
    void updateBelowBookedIsRejected() {
        Session session = service.addSession("Yoga", 5);
        Member first = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member second = service.addMember(MemberType.REGULAR, "Anna", 26);
        service.book(session.getId(), first.getId());
        service.book(session.getId(), second.getId());

        assertThrows(RuleViolationException.class, () -> service.updateSession(session.getId(), "Yoga", 1));

        assertEquals(5, service.getSession(session.getId()).getCapacity());
    }

    @Test
    @DisplayName("Граница: вместимость ровно по числу записанных допустима")
    void updateToExactlyBookedCountIsAllowed() {
        Session session = service.addSession("Yoga", 5);
        Member first = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member second = service.addMember(MemberType.REGULAR, "Anna", 26);
        service.book(session.getId(), first.getId());
        service.book(session.getId(), second.getId());

        Session updated = service.updateSession(session.getId(), "Yoga", 2);

        assertEquals(0, updated.getFreePlaces());
    }

    @Test
    @DisplayName("updateSession требует вместимость и корректные данные")
    void updateValidatesInput() {
        Session session = service.addSession("Yoga", 5);

        assertThrows(InvalidDataException.class, () -> service.updateSession(session.getId(), "Yoga", null));
        assertThrows(InvalidDataException.class, () -> service.updateSession(session.getId(), "", 5));
        assertThrows(InvalidDataException.class, () -> service.updateSession(session.getId(), "Yoga", 0));

        assertEquals("Yoga", service.getSession(session.getId()).getName());
        assertEquals(5, service.getSession(session.getId()).getCapacity());
    }

    @Test
    @DisplayName("Изменение несуществующего занятия даёт NotFound")
    void updateUnknownSessionFails() {
        assertThrows(NotFoundException.class, () -> service.updateSession(999, "X", 5));
    }

    // ---------- удаление ----------

    @Test
    @DisplayName("Пустое занятие удаляется")
    void deleteRemovesEmptySession() {
        Session session = service.addSession("Yoga", 5);

        service.deleteSession(session.getId());

        assertThrows(NotFoundException.class, () -> service.getSession(session.getId()));
        assertTrue(service.listSessions(null, null).isEmpty());
    }

    @Test
    @DisplayName("Удаление несуществующего занятия даёт NotFound")
    void deleteUnknownSessionFails() {
        assertThrows(NotFoundException.class, () -> service.deleteSession(999));
    }

    @Test
    @DisplayName("Нельзя удалить занятие, на которое кто-то записан")
    void cannotDeleteSessionWithMembers() {
        Session session = service.addSession("Yoga", 5);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.book(session.getId(), member.getId());

        assertThrows(RuleViolationException.class, () -> service.deleteSession(session.getId()));

        assertSame(session, service.getSession(session.getId()));
    }

    @Test
    @DisplayName("После отмены всех записей занятие удалить можно")
    void canDeleteSessionAfterCancel() {
        Session session = service.addSession("Yoga", 5);
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.book(session.getId(), member.getId());
        service.cancel(session.getId(), member.getId());

        service.deleteSession(session.getId());

        assertThrows(NotFoundException.class, () -> service.getSession(session.getId()));
    }
}
