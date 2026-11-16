package app.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    @Test
    @DisplayName("Новый клиент хранит данные, имя обрезается от пробелов")
    void createsMemberWithTrimmedName() {
        Member member = new RegularMember("  Ivan  ", 25, 7);

        assertEquals("Ivan", member.getName());
        assertEquals(25, member.getAge());
        assertEquals(7, member.getId());
        assertTrue(member.isAdult());
    }

    @Test
    @DisplayName("Пустое или отсутствующее имя запрещено")
    void rejectsBlankName() {
        assertThrows(InvalidDataException.class, () -> new RegularMember(null, 25, 1));
        assertThrows(InvalidDataException.class, () -> new RegularMember("", 25, 1));
        assertThrows(InvalidDataException.class, () -> new RegularMember("   ", 25, 1));
    }

    @Test
    @DisplayName("Возраст ниже 18 и выше 120 запрещён")
    void rejectsAgeOutOfRange() {
        assertThrows(InvalidDataException.class, () -> new RegularMember("Ivan", 17, 1));
        assertThrows(InvalidDataException.class, () -> new RegularMember("Ivan", 121, 1));
        assertThrows(InvalidDataException.class, () -> new RegularMember("Ivan", -5, 1));
    }

    @Test
    @DisplayName("Граничные возрасты 18 и 120 допустимы")
    void acceptsBoundaryAges() {
        assertEquals(18, new RegularMember("Ivan", 18, 1).getAge());
        assertEquals(120, new RegularMember("Ivan", 120, 2).getAge());
    }

    @Test
    @DisplayName("update меняет имя и возраст")
    void updateChangesNameAndAge() {
        Member member = new RegularMember("Ivan", 25, 1);

        member.update("  Petr ", 30);

        assertEquals("Petr", member.getName());
        assertEquals(30, member.getAge());
    }

    @Test
    @DisplayName("Неудачный update с плохим возрастом не меняет клиента")
    void failedUpdateWithBadAgeKeepsOldValues() {
        Member member = new RegularMember("Ivan", 25, 1);

        assertThrows(InvalidDataException.class, () -> member.update("Petr", 5));

        assertEquals("Ivan", member.getName());
        assertEquals(25, member.getAge());
    }

    @Test
    @DisplayName("Неудачный update с пустым именем не меняет клиента")
    void failedUpdateWithBlankNameKeepsOldValues() {
        Member member = new RegularMember("Ivan", 25, 1);

        assertThrows(InvalidDataException.class, () -> member.update(" ", 40));

        assertEquals("Ivan", member.getName());
        assertEquals(25, member.getAge());
    }

    @Test
    @DisplayName("Каждый вид абонемента сообщает свой тип")
    void eachSubclassReportsItsType() {
        assertEquals(MemberType.REGULAR, new RegularMember("A", 20, 1).type());
        assertEquals(MemberType.VIP, new VipMember("A", 20, 2).type());
        assertEquals(MemberType.SILVER, new SilverMember("A", 20, 3).type());
        assertEquals(MemberType.GOLD, new GoldMember("A", 20, 4).type());
    }
}
