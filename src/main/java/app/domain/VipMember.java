package app.domain;

public class VipMember extends Member {

    private int visits = 0;

    public VipMember(String name, int age, int id) {
        super(name, age, id);
    }

    @Override
    public boolean canBook() {
        return true;
    }

    @Override
    public void registerVisit() {
        visits++;
    }

    @Override
    public void cancelVisit() {
        if (visits == 0) {
            throw new RuleViolationException("Невозможно отменить посещение, так как оно не было зарегистрировано");
        }
        visits--;
    }

    @Override
    public MemberType type() {
        return MemberType.VIP;
    }

    @Override
    public int getVisits() {
        return visits;
    }
}
