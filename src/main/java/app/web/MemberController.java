package app.web;

import app.domain.MemberType;
import app.service.GymService;
import app.web.dto.MemberRequest;
import app.web.dto.MemberResponse;
import app.web.dto.MemberUpdateRequest;
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
@RequestMapping("/members")
public class MemberController {

    private final GymService service;

    public MemberController(GymService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemberResponse> list(@RequestParam Map<String, String> params) {
        QueryParams.allowOnly(params, "type", "name", "canBook");
        MemberType type = QueryParams.enumValue(params, "type", MemberType.class);
        Boolean canBook = QueryParams.bool(params, "canBook");
        return service.listMembers(type, params.get("name"), canBook).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(service.addMember(request.type(), request.name(), request.age()));
    }

    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable int id) {
        return MemberResponse.from(service.getMember(id));
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable int id, @Valid @RequestBody MemberUpdateRequest request) {
        return MemberResponse.from(service.updateMember(id, request.type(), request.name(), request.age()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        service.deleteMember(id);
    }

    @GetMapping("/{id}/sessions")
    public List<SessionResponse> sessions(@PathVariable int id) {
        return service.sessionsOf(id).stream().map(SessionResponse::from).toList();
    }
}
