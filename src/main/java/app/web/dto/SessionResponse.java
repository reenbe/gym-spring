package app.web.dto;

import app.domain.Session;

import java.util.List;

public record SessionResponse(int id, String name, int capacity, int freePlaces, List<Integer> memberIds) {

    public static SessionResponse from(Session s) {
        return new SessionResponse(s.getId(), s.getName(), s.getCapacity(),
                s.getFreePlaces(), s.getMemberIds());
    }
}
