package app.web;

import app.service.GymService;
import app.web.dto.BookRequest;
import app.web.dto.MemberResponse;
import app.web.dto.SessionRequest;
import app.web.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final GymService service;

    public SessionController(GymService service) {
        this.service = service;
    }

    @GetMapping
    public List<SessionResponse> list(@RequestParam Map<String, String> params) {
        QueryParams.allowOnly(params, "hasFreePlaces", "name");
        Boolean hasFreePlaces = QueryParams.bool(params, "hasFreePlaces");
        return service.listSessions(hasFreePlaces, params.get("name")).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody SessionRequest request) {
        return SessionResponse.from(service.addSession(request.name(), request.capacity()));
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable int id) {
        return SessionResponse.from(service.getSession(id));
    }

    @PutMapping("/{id}")
    public SessionResponse update(@PathVariable int id, @Valid @RequestBody SessionRequest request) {
        return SessionResponse.from(service.updateSession(id, request.name(), request.capacity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        service.deleteSession(id);
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> members(@PathVariable int id) {
        return service.membersOf(id).stream().map(MemberResponse::from).toList();
    }

    @PostMapping("/{id}/book")
    public SessionResponse book(@PathVariable int id, @Valid @RequestBody BookRequest request) {
        return SessionResponse.from(service.book(id, request.memberId()));
    }

    @PostMapping("/{id}/cancel")
    public SessionResponse cancel(@PathVariable int id, @Valid @RequestBody BookRequest request) {
        return SessionResponse.from(service.cancel(id, request.memberId()));
    }
}
