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

/** Клиенты: создание, чтение, фильтры, изменение, удаление. Без Spring, с настоящими in-memory репозиториями. */
class GymServiceMemberTest {

    private GymService service;

    @BeforeEach
    void setUp() {
        service = new GymService(new InMemoryMemberRepository(), new InMemorySessionRepository());
    }

    private static List<String> names(List<Member> members) {
        return members.stream().map(Member::getName).toList();
    }

    // ---------- создание ----------

    @Test
    @DisplayName("Клиентам выдаются последовательные id начиная с 1")
    void addMemberAssignsSequentialIds() {
        Member first = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member second = service.addMember(MemberType.VIP, "Anna", 30);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
    }

    @Test
    @DisplayName("Создаётся клиент запрошенного типа")
    void addMemberCreatesRequestedType() {
        for (MemberType type : MemberType.values()) {
            Member member = service.addMember(type, "Name", 25);
            assertEquals(type, member.type());
        }
    }

    @Test
    @DisplayName("Без типа или без возраста клиента не создать")
    void addMemberRequiresTypeAndAge() {
        assertThrows(InvalidDataException.class, () -> service.addMember(null, "Ivan", 25));
        assertThrows(InvalidDataException.class, () -> service.addMember(MemberType.GOLD, "Ivan", null));
    }

    @Test
    @DisplayName("Неверное имя или возраст дают ошибку и не занимают id")
    void failedCreationDoesNotConsumeId() {
        assertThrows(InvalidDataException.class, () -> service.addMember(MemberType.GOLD, "", 25));
        assertThrows(InvalidDataException.class, () -> service.addMember(MemberType.GOLD, "Bob", 17));

        Member member = service.addMember(MemberType.GOLD, "Bob", 25);

        assertEquals(1, member.getId());
        assertEquals(1, service.listMembers(null, null, null).size());
    }

    @Test
    @DisplayName("Граничные возрасты 18 и 120 принимаются, 17 и 121 нет")
    void ageBoundariesOnCreate() {
        assertThrows(InvalidDataException.class, () -> service.addMember(MemberType.REGULAR, "A", 17));
        assertThrows(InvalidDataException.class, () -> service.addMember(MemberType.REGULAR, "A", 121));
        service.addMember(MemberType.REGULAR, "A", 18);
        service.addMember(MemberType.REGULAR, "B", 120);

        assertEquals(2, service.listMembers(null, null, null).size());
    }

    // ---------- чтение ----------

    @Test
    @DisplayName("getMember возвращает созданного клиента")
    void getMemberReturnsStoredMember() {
        Member created = service.addMember(MemberType.VIP, "Anna", 30);

        assertSame(created, service.getMember(created.getId()));
    }

    @Test
    @DisplayName("getMember несуществующего клиента даёт NotFound")
    void getUnknownMemberFails() {
        assertThrows(NotFoundException.class, () -> service.getMember(999));
    }

    // ---------- список и фильтры ----------

    @Test
    @DisplayName("Без фильтров приходят все клиенты в порядке создания")
    void listWithoutFilters() {
        service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.addMember(MemberType.VIP, "Anna", 30);
        service.addMember(MemberType.GOLD, "Gold", 35);

        assertEquals(List.of("Ivan", "Anna", "Gold"), names(service.listMembers(null, null, null)));
    }

    @Test
    @DisplayName("Пустой список, если клиентов нет")
    void listWhenEmpty() {
        assertTrue(service.listMembers(null, null, null).isEmpty());
    }

    @Test
    @DisplayName("Фильтр по типу")
    void filterByType() {
        service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.addMember(MemberType.VIP, "Anna", 30);
        service.addMember(MemberType.VIP, "Boris", 31);

        assertEquals(List.of("Anna", "Boris"), names(service.listMembers(MemberType.VIP, null, null)));
    }

    @Test
    @DisplayName("Фильтр по части имени не зависит от регистра")
    void filterByNamePartIgnoresCase() {
        service.addMember(MemberType.REGULAR, "Ivan", 25);
        service.addMember(MemberType.REGULAR, "Anna", 30);

        assertEquals(List.of("Ivan"), names(service.listMembers(null, "IV", null)));
        assertEquals(List.of("Anna"), names(service.listMembers(null, "nn", null)));
    }

    @Test
    @DisplayName("Фильтр canBook отделяет клиентов с исчерпанным лимитом")
    void filterByCanBook() {
        service.addMember(MemberType.REGULAR, "Ivan", 25);
        Member silver = service.addMember(MemberType.SILVER, "Limit", 40);
        for (int i = 0; i < 8; i++) {
            silver.registerVisit();
        }

        assertEquals(List.of("Limit"), names(service.listMembers(null, null, false)));
        assertEquals(List.of("Ivan"), names(service.listMembers(null, null, true)));
    }

    @Test
    @DisplayName("Фильтры комбинируются")
    void combinedFilters() {
        service.addMember(MemberType.SILVER, "Limit One", 40);
        service.addMember(MemberType.SILVER, "Other", 41);
        service.addMember(MemberType.GOLD, "Limit Two", 42);

        assertEquals(List.of("Limit One"), names(service.listMembers(MemberType.SILVER, "limit", true)));
    }

    @Test
    @DisplayName("Если ничего не подошло, список пуст")
    void filterWithNoMatches() {
        service.addMember(MemberType.REGULAR, "Ivan", 25);

        assertTrue(service.listMembers(MemberType.GOLD, null, null).isEmpty());
        assertTrue(service.listMembers(null, "zzz", null).isEmpty());
    }

    // ---------- изменение ----------

    @Test
    @DisplayName("updateMember меняет имя и возраст, id и тип остаются")
    void updateChangesNameAndAge() {
        Member member = service.addMember(MemberType.VIP, "Anna", 30);

        Member updated = service.updateMember(member.getId(), MemberType.VIP, "Anna P", 31);

        assertEquals("Anna P", updated.getName());
        assertEquals(31, updated.getAge());
        assertEquals(MemberType.VIP, updated.type());
        assertEquals("Anna P", service.getMember(member.getId()).getName());
    }

    @Test
    @DisplayName("Тип в запросе на изменение можно не указывать")
    void updateWithoutTypeIsAllowed() {
        Member member = service.addMember(MemberType.GOLD, "Gold", 35);

        Member updated = service.updateMember(member.getId(), null, "Gold 2", 36);

        assertEquals("Gold 2", updated.getName());
    }

    @Test
    @DisplayName("Сменить тип абонемента нельзя, клиент не меняется")
    void updateCannotChangeType() {
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        assertThrows(InvalidDataException.class,
                () -> service.updateMember(member.getId(), MemberType.VIP, "Ivan 2", 26));

        assertEquals("Ivan", service.getMember(member.getId()).getName());
        assertEquals(25, service.getMember(member.getId()).getAge());
    }

    @Test
    @DisplayName("updateMember требует возраст и корректное имя")
    void updateValidatesInput() {
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        assertThrows(InvalidDataException.class, () -> service.updateMember(member.getId(), null, "Ivan", null));
        assertThrows(InvalidDataException.class, () -> service.updateMember(member.getId(), null, "", 25));
        assertThrows(InvalidDataException.class, () -> service.updateMember(member.getId(), null, "Ivan", 5));

        assertEquals("Ivan", service.getMember(member.getId()).getName());
    }

    @Test
    @DisplayName("Изменение несуществующего клиента даёт NotFound")
    void updateUnknownMemberFails() {
        assertThrows(NotFoundException.class, () -> service.updateMember(999, null, "X", 25));
    }

    // ---------- удаление ----------

    @Test
    @DisplayName("Удалённый клиент исчезает из хранилища и списка")
    void deleteRemovesMember() {
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);

        service.deleteMember(member.getId());

        assertThrows(NotFoundException.class, () -> service.getMember(member.getId()));
        assertTrue(service.listMembers(null, null, null).isEmpty());
    }

    @Test
    @DisplayName("Удаление несуществующего клиента даёт NotFound")
    void deleteUnknownMemberFails() {
        assertThrows(NotFoundException.class, () -> service.deleteMember(999));
    }

    @Test
    @DisplayName("Нельзя удалить клиента, записанного на занятие")
    void cannotDeleteBookedMember() {
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Session session = service.addSession("Yoga", 5);
        service.book(session.getId(), member.getId());

        assertThrows(RuleViolationException.class, () -> service.deleteMember(member.getId()));

        assertSame(member, service.getMember(member.getId()));
    }

    @Test
    @DisplayName("После отмены записи клиента удалить можно")
    void canDeleteMemberAfterCancel() {
        Member member = service.addMember(MemberType.REGULAR, "Ivan", 25);
        Session session = service.addSession("Yoga", 5);
        service.book(session.getId(), member.getId());
        service.cancel(session.getId(), member.getId());

        service.deleteMember(member.getId());

        assertThrows(NotFoundException.class, () -> service.getMember(member.getId()));
    }
}
