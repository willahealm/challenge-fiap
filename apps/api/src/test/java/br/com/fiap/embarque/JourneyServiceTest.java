package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class JourneyServiceTest {
    static class MutableClock extends Clock {
        Instant now=Instant.parse("2026-09-13T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now,zone); }
        public Instant instant() { return now; }
    }
    MutableClock clock; DemoAuth auth; DemoRepository db; JourneyService service;
    final Actor lucas=new Actor("user-lucas","Lucas","PASSENGER",null,null);
    final Actor device=new Actor("device-demo","Totem","TOTEM",null,"totem-tiete-01");
    final Actor operator=new Actor("operator-demo","Operador","OPERATOR",null,null);
    @BeforeEach void setup() { clock=new MutableClock();auth=new DemoAuth(clock);db=new DemoRepository(clock);service=new JourneyService(db,auth,clock); }
    @Test void handoffIsHashedSingleUseAndConfirmsPhysicalPoint() {
        var handoff=service.handoff("journey-demo",lucas);
        assertThat(db.handoffs).doesNotContainKey(handoff.token()).containsKey(DemoAuth.hash(handoff.token()));
        var consumed=service.consume("totem-tiete-01",handoff.token(),device);
        assertThat(consumed.journey().currentPointId()).isEqualTo("point-totem-01");
        assertThat(service.view("trip-demo",lucas).journey()).isEqualTo(consumed.journey());
        assertThatThrownBy(() -> service.consume("totem-tiete-01",handoff.token(),device)).isInstanceOf(ApiException.class).hasMessageContaining("utilizado");
        var kiosk=auth.authenticate(consumed.accessToken());
        assertThatThrownBy(() -> service.view("trip-ana",kiosk)).isInstanceOf(ApiException.class);
        clock.now=clock.now.plusSeconds(30);
        assertThatThrownBy(() -> auth.authenticate(consumed.accessToken())).isInstanceOf(ApiException.class);
    }
    @Test void handoffExpiresExactlyAtSixtySeconds() {
        var h=service.handoff("journey-demo",lucas);clock.now=clock.now.plusSeconds(60);
        assertThatThrownBy(() -> service.consume("totem-tiete-01",h.token(),device)).isInstanceOf(ApiException.class);
    }
    @Test void replacementRevokesOldHandoff() {
        var first=service.handoff("journey-demo",lucas);var second=service.handoff("journey-demo",lucas);
        assertThatThrownBy(() -> service.consume("totem-tiete-01",first.token(),device)).isInstanceOf(ApiException.class);
        assertThat(service.consume("totem-tiete-01",second.token(),device)).isNotNull();
    }
    @Test void shortCodeConsumesBothAliases() {
        var h=service.handoff("journey-demo",lucas);assertThat(h.code()).matches("[A-HJ-NP-Z2-9]{6}");
        assertThat(db.handoffs).doesNotContainKey(h.code()).containsKey(DemoAuth.hash(h.code()));
        service.consume("totem-tiete-01",h.code(),device);
        assertThatThrownBy(() -> service.consume("totem-tiete-01",h.token(),device)).isInstanceOf(ApiException.class);
    }
    @Test void kioskActivityRenewsIdleExpiryButNeverBeyondFiveMinutes() {
        var h=service.handoff("journey-demo",lucas);var c=service.consume("totem-tiete-01",h.token(),device);
        var start=clock.now;
        for(int i=0;i<14;i++) { clock.now=clock.now.plusSeconds(20);auth.touch(c.accessToken()); }
        assertThat(auth.touch(c.accessToken()).get("expiresAt")).isEqualTo(start.plusSeconds(300));
        clock.now=start.plusSeconds(300);
        assertThatThrownBy(() -> auth.authenticate(c.accessToken())).isInstanceOf(ApiException.class);
    }
    @Test void concurrentConsumptionHasExactlyOneWinner() throws Exception {
        String token=service.handoff("journey-demo",lucas).token();
        try(var executor=Executors.newFixedThreadPool(8)) {
            var start=new CountDownLatch(1);var tasks=new ArrayList<Future<Boolean>>();
            for(int i=0;i<8;i++) tasks.add(executor.submit(() -> { start.await();try { service.consume("totem-tiete-01",token,device);return true; }catch(ApiException e){return false;} }));
            start.countDown();int wins=0;for(var result:tasks) if(result.get(5,TimeUnit.SECONDS)) wins++;
            assertThat(wins).isEqualTo(1);
        }
    }
    @Test void platformChangeAtomicallyChangesRouteAndAddsAlert() {
        var alert=service.alert("trip-demo",new AlertRequest("PLATFORM_CHANGE","CRITICAL","Vá à plataforma 21.","21"),operator);
        var view=service.view("trip-demo",lucas);
        assertThat(view.trip().platform()).isEqualTo("21");assertThat(view.route().toPointId()).isEqualTo("point-platform-21");
        assertThat(view.alerts()).containsExactly(alert);assertThat(alert.previousPlatform()).isEqualTo("18");
    }
    @Test void helpRequiresBoundJourneyAndCarriesContext() {
        assertThatThrownBy(() -> service.help("totem-tiete-01",new HelpRequestBody("journey-demo","MOBILITY"),device)).isInstanceOf(ApiException.class);
        var consumed=service.consume("totem-tiete-01",service.handoff("journey-demo",lucas).token(),device);
        var help=service.help("totem-tiete-01",new HelpRequestBody(null,"MOBILITY"),auth.authenticate(consumed.accessToken()));
        assertThat(help.tripId()).isEqualTo("trip-demo");assertThat(help.pointId()).isEqualTo("point-totem-01");
        assertThat(service.helpRequests(operator)).containsExactly(help);
    }
    @Test void resetIsIdempotentAndRevokesCredentialsAndHandoffs() {
        var login=auth.issue(lucas,Duration.ofHours(1));var h=service.handoff("journey-demo",lucas);
        service.alert("trip-demo",new AlertRequest("PLATFORM_CHANGE","CRITICAL","Mudou","21"),operator);
        service.reset(operator);service.reset(operator);
        assertThat(service.trips(operator)).hasSize(2);assertThat(service.view("trip-demo",lucas).trip().platform()).isEqualTo("18");
        assertThat(service.view("trip-demo",lucas).alerts()).isEmpty();
        assertThatThrownBy(() -> auth.authenticate(login.accessToken())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.consume("totem-tiete-01",h.token(),device)).isInstanceOf(ApiException.class);
    }
    @Test void feedbackRequiresCompletionAndUpsertsPerJourney() {
        var request=new FeedbackRequest("journey-demo",5,List.of("orientação"),"Tudo certo");
        assertThatThrownBy(() -> service.feedback(request,lucas)).isInstanceOf(ApiException.class);
        service.complete("journey-demo",lucas);service.complete("journey-demo",lucas);
        service.feedback(request,lucas);service.feedback(request,lucas);
        assertThat(db.feedback).hasSize(1);assertThat(service.view("trip-demo",lucas).trip().activeJourneys()).isZero();
    }
    @Test void checkpointRejectsOtherTerminalAndUnknownChecklistKey() {
        assertThatThrownBy(() -> service.checkpoint("journey-demo","RIO-ENTRADA",lucas)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.checklist("journey-demo",new ChecklistRequest(Map.of("admin",true)),lucas)).isInstanceOf(ApiException.class);
    }
    @Test void rateLimitRejectsEleventhHandoffUntilWindowEnds() {
        for(int i=0;i<10;i++) service.handoff("journey-demo",lucas);
        assertThatThrownBy(() -> service.handoff("journey-demo",lucas)).isInstanceOf(ApiException.class).hasMessageContaining("minuto");
        clock.now=clock.now.plusSeconds(60);assertThat(service.handoff("journey-demo",lucas)).isNotNull();
    }
}
