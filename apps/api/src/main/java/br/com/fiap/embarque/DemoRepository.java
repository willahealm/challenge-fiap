package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** All accesses are guarded by JourneyService's monitor, including reset and consume. */
@Repository @Profile("demo")
public class DemoRepository {
    record Handoff(String journeyId, Instant expiresAt) {}
    final Map<String,Trip> trips=new LinkedHashMap<>();
    final Map<String,Journey> journeys=new LinkedHashMap<>();
    final Map<String,Point> points=new LinkedHashMap<>();
    final Map<String,Totem> totems=new LinkedHashMap<>();
    final Map<String,Handoff> handoffs=new HashMap<>();
    final List<Alert> alerts=new ArrayList<>();
    final Map<String,HelpRequest> help=new LinkedHashMap<>();
    final Map<String,Feedback> feedback=new LinkedHashMap<>();
    private final Clock clock;
    public DemoRepository(Clock clock) { this.clock=clock; reset(); }
    void reset() {
        trips.clear(); journeys.clear(); points.clear(); totems.clear(); handoffs.clear(); alerts.clear(); help.clear(); feedback.clear();
        var departure=LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo"))).plusDays(1)
                .atTime(18,30).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
        trips.put("trip-demo",new Trip("trip-demo","terminal-tiete","São Paulo · Tietê","Rio de Janeiro · Novo Rio",departure,"Viação demonstração","18","ON_TIME",1));
        trips.put("trip-ana",new Trip("trip-ana","terminal-tiete","São Paulo · Tietê","Curitiba",departure.plusSeconds(3600),"Viação demonstração","18","ON_TIME",1));
        journeys.put("journey-demo",new Journey("journey-demo","trip-demo","user-lucas","point-entrance","PREPARING",Map.of("document",false,"ticket",false,"luggage",false,"departureTime",false),clock.instant()));
        journeys.put("journey-ana",new Journey("journey-ana","trip-ana","user-ana","point-entrance","PREPARING",Map.of("document",false,"ticket",false,"luggage",false,"departureTime",false),clock.instant()));
        points.put("TIETE-ENTRADA",new Point("point-entrance","terminal-tiete","TIETE-ENTRADA","Entrada principal","ENTRANCE"));
        points.put("TIETE-TOTEM-01",new Point("point-totem-01","terminal-tiete","TIETE-TOTEM-01","Totem do saguão","TOTEM"));
        points.put("TIETE-SETOR-B",new Point("point-sector-b","terminal-tiete","TIETE-SETOR-B","Setor B","LANDMARK"));
        points.put("TIETE-P18",new Point("point-platform-18","terminal-tiete","TIETE-P18","Plataforma 18","PLATFORM"));
        points.put("TIETE-P21",new Point("point-platform-21","terminal-tiete","TIETE-P21","Plataforma 21","PLATFORM"));
        points.put("RIO-ENTRADA",new Point("point-rio-entrance","terminal-rio","RIO-ENTRADA","Entrada Novo Rio","ENTRANCE"));
        totems.put("totem-tiete-01",new Totem("totem-tiete-01","terminal-tiete","point-totem-01","Totem do saguão Tietê","ONLINE"));
    }
}
