package app.web.dto;

import app.domain.Member;

public record MemberResponse(int id, String type, String name, int age, int visits, boolean canBook) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.type().name(), m.getName(), m.getAge(),
                m.getVisits(), m.canBook());
    }
}
