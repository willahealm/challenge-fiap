package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service @Profile("demo")
public class JourneyService {
    private final DemoRepository db;
    private final DemoAuth auth;
    private final Clock clock;
    public JourneyService(DemoRepository db,DemoAuth auth,Clock clock) { this.db=db;this.auth=auth;this.clock=clock; }
    public synchronized Trip next(Actor actor) {
        passenger(actor);
        return db.journeys.values().stream().filter(j -> j.userId().equals(actor.id()) && !j.stage().equals("COMPLETED"))
                .map(j -> db.trips.get(j.tripId())).filter(t -> t.departureAt().isAfter(clock.instant()))
                .min(Comparator.comparing(Trip::departureAt)).orElseThrow(ApiException::notFound);
    }
    public synchronized JourneyView view(String tripId,Actor actor) {
        var journey=db.journeys.values().stream().filter(j -> j.tripId().equals(tripId)).findFirst().orElseThrow(ApiException::notFound);
        access(journey,actor); return view(journey);
    }
    private JourneyView view(Journey j) {
        var trip=db.trips.get(j.tripId());
        var destination="point-platform-"+trip.platform();
        var steps=new ArrayList<Step>();
        if(!destination.equals(j.currentPointId())) {
            if(!"point-sector-b".equals(j.currentPointId())) steps.add(new Step("point-sector-b","Siga pelo corredor acessível até o setor B.",35,90));
            steps.add(new Step(destination,"No setor B, vire à direita e siga até a plataforma "+trip.platform()+".",trip.platform().equals("21")?65:40,180));
        }
        var route=new Route("route-"+trip.platform(),trip.terminalId(),j.currentPointId(),destination,true,
                steps.stream().mapToInt(Step::distanceMeters).sum(),steps.isEmpty()?0:steps.getFirst().headingDegrees(),List.copyOf(steps));
        return new JourneyView(j,trip,route,db.alerts.stream().filter(a -> a.tripId().equals(trip.id())).toList());
    }
    public synchronized JourneyView checklist(String id,ChecklistRequest request,Actor actor) {
        var j=journey(id,actor); passenger(actor); open(j);
        if(!j.checklist().keySet().containsAll(request.checklist().keySet())) throw ApiException.invalid("Item de checklist desconhecido.");
        var values=new LinkedHashMap<>(j.checklist());values.putAll(request.checklist());
        return save(new Journey(j.id(),j.tripId(),j.userId(),j.currentPointId(),j.stage(),Map.copyOf(values),clock.instant()));
    }
    public synchronized JourneyView checkpoint(String id,String code,Actor actor) {
        passenger(actor); var j=journey(id,actor);open(j);
        var point=db.points.get(code.trim().toUpperCase(Locale.ROOT));
        if(point==null || !point.terminalId().equals(db.trips.get(j.tripId()).terminalId())) throw ApiException.invalid("Código inválido ou de outro terminal.");
        return save(atPoint(j,point.id()));
    }
    private Journey atPoint(Journey j,String point) { return new Journey(j.id(),j.tripId(),j.userId(),point,"AT_TERMINAL",j.checklist(),clock.instant()); }
    public synchronized HandoffResponse handoff(String id,Actor actor) {
        passenger(actor);var j=journey(id,actor);open(j); auth.limit("issue:"+actor.id(),10);
        db.handoffs.values().removeIf(h -> h.journeyId().equals(id) || !clock.instant().isBefore(h.expiresAt()));
        String token=auth.randomToken();String code;
        do { code=auth.shortCode(); } while(db.handoffs.containsKey(DemoAuth.hash(code)));
        var expiry=clock.instant().plusSeconds(60);var handoff=new DemoRepository.Handoff(id,expiry);
        db.handoffs.put(DemoAuth.hash(token),handoff);db.handoffs.put(DemoAuth.hash(code),handoff);
        return new HandoffResponse(token,code,expiry);
    }
    public synchronized ConsumeResponse consume(String totemId,String token,Actor actor) {
        device(totemId,actor); auth.limit("consume:"+actor.id(),10);
        var totem=db.totems.get(totemId); if(totem==null) throw ApiException.notFound();
        var hash=DemoAuth.hash(token);var handoff=db.handoffs.get(hash);
        if(handoff==null || !clock.instant().isBefore(handoff.expiresAt())) { db.handoffs.remove(hash); throw new ApiException(HttpStatus.GONE,"HANDOFF_UNAVAILABLE","Código expirado, inválido ou já utilizado."); }
        var j=db.journeys.get(handoff.journeyId());open(j);
        if(!db.trips.get(j.tripId()).terminalId().equals(totem.terminalId())) throw ApiException.invalid("Totem de outro terminal.");
        db.handoffs.values().removeIf(h -> h.equals(handoff));
        var view=save(atPoint(j,totem.pointId()));
        var session=auth.issue(new Actor("kiosk-"+UUID.randomUUID(),"Sessão do totem","KIOSK",j.id(),totemId),Duration.ofSeconds(30));
        return new ConsumeResponse(session.accessToken(),session.expiresAt(),view.journey(),view.trip(),view.route(),view.alerts());
    }
    public synchronized JourneyView complete(String id,Actor actor) {
        var j=journey(id,actor); if(!Set.of("PASSENGER","KIOSK").contains(actor.role())) throw ApiException.forbidden();
        if(j.stage().equals("COMPLETED")) return view(j);
        var target="point-platform-"+db.trips.get(j.tripId()).platform();
        var result=save(new Journey(j.id(),j.tripId(),j.userId(),target,"COMPLETED",j.checklist(),clock.instant()));
        db.handoffs.values().removeIf(h -> h.journeyId().equals(id));
        var t=db.trips.get(j.tripId());db.trips.put(t.id(),new Trip(t.id(),t.terminalId(),t.origin(),t.destination(),t.departureAt(),t.carrier(),t.platform(),t.status(),0));
        return view(result.journey());
    }
    public synchronized Feedback feedback(FeedbackRequest request,Actor actor) {
        passenger(actor);var j=journey(request.journeyId(),actor);
        if(!j.stage().equals("COMPLETED")) throw conflict("Conclua a jornada antes de avaliar.");
        var feedback=new Feedback("feedback-"+j.id(),j.id(),request.rating(),request.tags()==null?List.of():List.copyOf(request.tags()),request.comment()==null?"":request.comment(),clock.instant());
        db.feedback.put(j.id(),feedback); return feedback;
    }
    public synchronized List<Trip> trips(Actor actor) { operator(actor);return List.copyOf(db.trips.values()); }
    public synchronized Alert alert(String id,AlertRequest request,Actor actor) {
        operator(actor);var trip=db.trips.get(id);if(trip==null) throw ApiException.notFound();
        boolean change=request.type().equals("PLATFORM_CHANGE");
        if(change && request.platform()==null) throw ApiException.invalid("Informe a nova plataforma.");
        if(!change && request.platform()!=null) throw ApiException.invalid("Use PLATFORM_CHANGE para alterar a plataforma.");
        if(change && trip.platform().equals(request.platform())) throw conflict("A viagem já está nesta plataforma.");
        String platform=change?request.platform():trip.platform();
        String status=request.type().equals("DELAY")?"DELAYED":request.type().equals("BOARDING")?"BOARDING":trip.status();
        db.trips.put(id,new Trip(trip.id(),trip.terminalId(),trip.origin(),trip.destination(),trip.departureAt(),trip.carrier(),platform,status,trip.activeJourneys()));
        var alert=new Alert(UUID.randomUUID().toString(),id,request.type(),request.severity(),request.message(),trip.platform(),platform,clock.instant());db.alerts.add(alert);return alert;
    }
    public synchronized HelpRequest help(String id,HelpRequestBody request,Actor actor) {
        if(!Set.of("TOTEM","KIOSK").contains(actor.role()) || !id.equals(actor.totemId())) throw ApiException.forbidden();
        var totem=db.totems.get(id);if(totem==null) throw ApiException.notFound();
        String journeyId=request.journeyId()==null?actor.journeyId():request.journeyId();
        if(actor.role().equals("TOTEM") && journeyId!=null) throw ApiException.forbidden();
        Journey j=journeyId==null?null:journey(journeyId,actor);
        auth.limit("help:"+id,10);
        var help=new HelpRequest(UUID.randomUUID().toString(),journeyId,j==null?null:j.tripId(),id,totem.pointId(),request.category(),"OPEN",clock.instant());
        db.help.put(help.id(),help);return help;
    }
    public synchronized List<HelpRequest> helpRequests(Actor actor) { operator(actor);return List.copyOf(db.help.values()); }
    public synchronized HelpRequest helpStatus(String id,String status,Actor actor) {
        operator(actor);var h=db.help.get(id);if(h==null) throw ApiException.notFound();
        if(h.status().equals("RESOLVED") && !status.equals("RESOLVED")) throw conflict("Chamado já resolvido.");
        var updated=new HelpRequest(h.id(),h.journeyId(),h.tripId(),h.totemId(),h.pointId(),h.category(),status,h.createdAt());db.help.put(id,updated);return updated;
    }
    public synchronized List<Point> points() { return db.points.values().stream().filter(p -> p.terminalId().equals("terminal-tiete")).toList(); }
    public synchronized Map<String,Object> reset(Actor actor) { operator(actor);db.reset();auth.revokeAll();return Map.of("status","RESET","tripId","trip-demo","journeyId","journey-demo"); }
    private Journey journey(String id,Actor actor) { var j=db.journeys.get(id);if(j==null) throw ApiException.notFound();access(j,actor);return j; }
    private void access(Journey j,Actor actor) {
        if(actor.role().equals("PASSENGER") && j.userId().equals(actor.id())) return;
        if(actor.role().equals("KIOSK") && j.id().equals(actor.journeyId())) return;
        throw ApiException.forbidden();
    }
    private void passenger(Actor actor) { if(!actor.role().equals("PASSENGER")) throw ApiException.forbidden(); }
    private void operator(Actor actor) { if(!actor.role().equals("OPERATOR")) throw ApiException.forbidden(); }
    private void device(String id,Actor actor) { if(!actor.role().equals("TOTEM") || !id.equals(actor.totemId())) throw ApiException.forbidden(); }
    private void open(Journey j) { if(j.stage().equals("COMPLETED")) throw conflict("Jornada já concluída."); }
    private ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT,"CONFLICT",message); }
    private JourneyView save(Journey journey) { db.journeys.put(journey.id(),journey);return view(journey); }
}
