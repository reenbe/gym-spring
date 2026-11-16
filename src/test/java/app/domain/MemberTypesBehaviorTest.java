package app.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTypesBehaviorTest {

    @Test
    @DisplayName("REGULAR: можно записываться всегда, посещения не считаются")
    void regularHasNoLimitAndNoCounting() {
        Member member = new RegularMember("Ivan", 25, 1);

        for (int i = 0; i < 50; i++) {
            member.registerVisit();
        }

        assertTrue(member.canBook());
        assertEquals(0, member.getVisits());
    }

    @Test
    @DisplayName("VIP: без лимита, но посещения считаются")
    void vipCountsVisitsWithoutLimit() {
        Member member = new VipMember("Anna", 30, 1);

        for (int i = 0; i < 100; i++) {
            member.registerVisit();
        }

        assertTrue(member.canBook());
        assertEquals(100, member.getVisits());
    }

    @Test
    @DisplayName("VIP: отмена уменьшает счётчик, отмена без посещений запрещена")
    void vipCancelDecrementsAndRejectsWhenZero() {
        Member member = new VipMember("Anna", 30, 1);
        member.registerVisit();
        member.registerVisit();

        member.cancelVisit();
        assertEquals(1, member.getVisits());

        member.cancelVisit();
        assertEquals(0, member.getVisits());
        assertThrows(RuleViolationException.class, member::cancelVisit);
        assertEquals(0, member.getVisits());
    }

    @Test
    @DisplayName("SILVER: лимит 8, на границе 7 и 8 посещений")
    void silverLimitBoundary() {
        LimitedMember member = new SilverMember("Limit", 40, 1);

        for (int i = 0; i < 7; i++) {
            member.registerVisit();
        }
        assertTrue(member.canBook());
        assertEquals(1, member.remainingVisits());

        member.registerVisit();
        assertFalse(member.canBook());
        assertEquals(0, member.remainingVisits());
        assertEquals(8, member.getVisits());
    }

    @Test
    @DisplayName("SILVER: девятое посещение запрещено и счётчик не меняется")
    void silverRejectsVisitOverLimit() {
        Member member = new SilverMember("Limit", 40, 1);
        for (int i = 0; i < 8; i++) {
            member.registerVisit();
        }

        assertThrows(RuleViolationException.class, member::registerVisit);

        assertEquals(8, member.getVisits());
    }

    @Test
    @DisplayName("SILVER: после отмены лимит снова позволяет записаться")
    void silverCancelFreesOneVisit() {
        Member member = new SilverMember("Limit", 40, 1);
        for (int i = 0; i < 8; i++) {
            member.registerVisit();
        }
        assertFalse(member.canBook());

        member.cancelVisit();

        assertTrue(member.canBook());
        assertEquals(7, member.getVisits());
    }

    @Test
    @DisplayName("SILVER: отмена без посещений запрещена")
    void silverCancelWithoutVisitsIsRejected() {
        Member member = new SilverMember("Limit", 40, 1);

        assertThrows(RuleViolationException.class, member::cancelVisit);
        assertEquals(0, member.getVisits());
    }

    @Test
    @DisplayName("GOLD: лимит 12, тринадцатое посещение запрещено")
    void goldLimitIsTwelve() {
        LimitedMember member = new GoldMember("Gold", 35, 1);

        for (int i = 0; i < 12; i++) {
            member.registerVisit();
        }

        assertFalse(member.canBook());
        assertEquals(0, member.remainingVisits());
        assertThrows(RuleViolationException.class, member::registerVisit);
        assertEquals(12, member.getVisits());
    }

    @Test
    @DisplayName("Лимиты разные: на девятом посещении SILVER отказывает, GOLD пропускает")
    void silverAndGoldDifferInLimit() {
        Member silver = new SilverMember("S", 30, 1);
        Member gold = new GoldMember("G", 30, 2);
        for (int i = 0; i < 8; i++) {
            silver.registerVisit();
            gold.registerVisit();
        }

        assertThrows(RuleViolationException.class, silver::registerVisit);
        gold.registerVisit();

        assertEquals(9, gold.getVisits());
    }
}
