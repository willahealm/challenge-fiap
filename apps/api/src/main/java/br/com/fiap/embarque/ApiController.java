package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1") @Profile("demo")
class ApiController {
    private final JourneyService service;
    private final DemoAuth auth;
    ApiController(JourneyService service,DemoAuth auth) { this.service=service;this.auth=auth; }
    @PostMapping("/auth/demo") AuthResponse login(@Valid @RequestBody Login login,HttpServletRequest request) { return auth.login(login,request.getRemoteAddr()); }
    @DeleteMapping("/auth/session") Map<String,String> logout(@RequestHeader("Authorization") String header) { auth.revoke(header.substring(7));return Map.of("status","SIGNED_OUT"); }
    @PostMapping("/auth/session/touch") Map<String,java.time.Instant> touch(@RequestHeader("Authorization") String header) { return auth.touch(header.substring(7)); }
    @GetMapping("/me/trips/next") Trip next(@AuthenticationPrincipal Actor actor) { return service.next(actor); }
    @GetMapping("/trips/{id}/journey") JourneyView view(@PathVariable String id,@AuthenticationPrincipal Actor actor) { return service.view(id,actor); }
    @PatchMapping("/journeys/{id}/checklist") JourneyView checklist(@PathVariable String id,@Valid @RequestBody ChecklistRequest request,@AuthenticationPrincipal Actor actor) { return service.checklist(id,request,actor); }
    @PostMapping("/journeys/{id}/checkpoints") JourneyView checkpoint(@PathVariable String id,@Valid @RequestBody CheckpointRequest request,@AuthenticationPrincipal Actor actor) { return service.checkpoint(id,request.code(),actor); }
    @PostMapping("/journeys/{id}/handoffs") HandoffResponse handoff(@PathVariable String id,@AuthenticationPrincipal Actor actor) { return service.handoff(id,actor); }
    @PostMapping("/totems/{id}/handoffs/consume") ConsumeResponse consume(@PathVariable String id,@Valid @RequestBody ConsumeRequest request,@AuthenticationPrincipal Actor actor) { return service.consume(id,request.token(),actor); }
    @PostMapping("/totems/{id}/help-requests") HelpRequest help(@PathVariable String id,@Valid @RequestBody HelpRequestBody request,@AuthenticationPrincipal Actor actor) { return service.help(id,request,actor); }
    @PostMapping("/journeys/{id}/complete") JourneyView complete(@PathVariable String id,@AuthenticationPrincipal Actor actor) { return service.complete(id,actor); }
    @PostMapping("/feedback") Feedback feedback(@Valid @RequestBody FeedbackRequest request,@AuthenticationPrincipal Actor actor) { return service.feedback(request,actor); }
    @GetMapping("/ops/trips") List<Trip> trips(@AuthenticationPrincipal Actor actor) { return service.trips(actor); }
    @PostMapping("/ops/trips/{id}/alerts") Alert alert(@PathVariable String id,@Valid @RequestBody AlertRequest request,@AuthenticationPrincipal Actor actor) { return service.alert(id,request,actor); }
    @GetMapping("/ops/help-requests") List<HelpRequest> helpRequests(@AuthenticationPrincipal Actor actor) { return service.helpRequests(actor); }
    @PatchMapping("/ops/help-requests/{id}") HelpRequest helpStatus(@PathVariable String id,@Valid @RequestBody HelpStatusRequest request,@AuthenticationPrincipal Actor actor) { return service.helpStatus(id,request.status(),actor); }
    @GetMapping("/terminal/points") List<Point> points() { return service.points(); }
    @PostMapping("/demo/reset") Map<String,Object> reset(@AuthenticationPrincipal Actor actor) { return service.reset(actor); }
}
