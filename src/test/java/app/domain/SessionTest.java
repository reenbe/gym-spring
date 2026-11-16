package app.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    @Test
    @DisplayName("Новое занятие пустое, свободны все места, имя обрезается")
    void newSessionIsEmpty() {
        Session session = new Session(3, "  Yoga ", 10);

        assertEquals(3, session.getId());
        assertEquals("Yoga", session.getName());
        assertEquals(10, session.getCapacity());
        assertEquals(10, session.getFreePlaces());
        assertTrue(session.isEmpty());
        assertTrue(session.hasFreePlaces());
    }

    @Test
    @DisplayName("Пустое название запрещено")
    void rejectsBlankName() {
        assertThrows(InvalidDataException.class, () -> new Session(1, null, 5));
        assertThrows(InvalidDataException.class, () -> new Session(1, "", 5));
        assertThrows(InvalidDataException.class, () -> new Session(1, "  ", 5));
    }

    @Test
    @DisplayName("Вместимость вне диапазона 1..500 запрещена, границы допустимы")
    void capacityBoundaries() {
        assertThrows(InvalidDataException.class, () -> new Session(1, "Box", 0));
        assertThrows(InvalidDataException.class, () -> new Session(1, "Box", -1));
        assertThrows(InvalidDataException.class, () -> new Session(1, "Box", 501));

        assertEquals(1, new Session(1, "Box", 1).getCapacity());
        assertEquals(500, new Session(2, "Box", 500).getCapacity());
    }

    @Test
    @DisplayName("Запись уменьшает число свободных мест")
    void addMemberTakesAPlace() {
        Session session = new Session(1, "Yoga", 2);

        session.addMember(10);

        assertTrue(session.isBooked(10));
        assertFalse(session.isEmpty());
        assertEquals(1, session.getFreePlaces());
        assertEquals(List.of(10), session.getMemberIds());
    }

    @Test
    @DisplayName("Один клиент не может записаться дважды")
    void cannotBookTwice() {
        Session session = new Session(1, "Yoga", 5);
        session.addMember(10);

        assertThrows(RuleViolationException.class, () -> session.addMember(10));

        assertEquals(4, session.getFreePlaces());
    }

    @Test
    @DisplayName("Граница вместимости: последнее место занять можно, лишнее нельзя")
    void cannotExceedCapacity() {
        Session session = new Session(1, "Yoga", 2);
        session.addMember(1);
        session.addMember(2);

        assertFalse(session.hasFreePlaces());
        assertEquals(0, session.getFreePlaces());
        assertThrows(RuleViolationException.class, () -> session.addMember(3));
        assertFalse(session.isBooked(3));
    }

    @Test
    @DisplayName("Отмена освобождает место")
    void removeMemberFreesAPlace() {
        Session session = new Session(1, "Yoga", 1);
        session.addMember(1);

        session.removeMember(1);

        assertTrue(session.isEmpty());
        assertTrue(session.hasFreePlaces());
        assertFalse(session.isBooked(1));
    }

    @Test
    @DisplayName("Нельзя убрать того, кто не записан")
    void cannotRemoveAbsentMember() {
        Session session = new Session(1, "Yoga", 3);

        assertThrows(RuleViolationException.class, () -> session.removeMember(99));
    }

    @Test
    @DisplayName("update меняет название и вместимость")
    void updateChangesNameAndCapacity() {
        Session session = new Session(1, "Yoga", 5);

        session.update("Pilates", 8);

        assertEquals("Pilates", session.getName());
        assertEquals(8, session.getCapacity());
        assertEquals(8, session.getFreePlaces());
    }

    @Test
    @DisplayName("Нельзя сделать мест меньше, чем уже записано, состояние не меняется")
    void updateBelowBookedIsRejected() {
        Session session = new Session(1, "Yoga", 5);
        session.addMember(1);
        session.addMember(2);

        assertThrows(RuleViolationException.class, () -> session.update("New", 1));

        assertEquals("Yoga", session.getName());
        assertEquals(5, session.getCapacity());
    }

    @Test
    @DisplayName("Граница update: вместимость ровно по числу записанных допустима")
    void updateToExactlyBookedCountIsAllowed() {
        Session session = new Session(1, "Yoga", 5);
        session.addMember(1);
        session.addMember(2);

        session.update("Yoga", 2);

        assertEquals(2, session.getCapacity());
        assertFalse(session.hasFreePlaces());
    }

    @Test
    @DisplayName("update с неверными данными не меняет занятие")
    void updateWithInvalidDataKeepsOldValues() {
        Session session = new Session(1, "Yoga", 5);

        assertThrows(InvalidDataException.class, () -> session.update("", 5));
        assertThrows(InvalidDataException.class, () -> session.update("Yoga", 0));

        assertEquals("Yoga", session.getName());
        assertEquals(5, session.getCapacity());
    }

    @Test
    @DisplayName("getMemberIds возвращает копию: снаружи список не изменить")
    void memberIdsAreACopy() {
        Session session = new Session(1, "Yoga", 5);
        session.addMember(1);

        session.getMemberIds().add(42);

        assertEquals(List.of(1), session.getMemberIds());
    }
}
