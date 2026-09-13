package br.com.fiap.embarquefacil.data.model;

import java.util.ArrayList;
import java.util.List;

public class JourneyDetails {
    public Journey journey;
    public Trip trip;
    public Route route;
    public List<OperationalAlert> alerts = new ArrayList<>();

    public OperationalAlert latestAlert() {
        return alerts == null || alerts.isEmpty() ? null : alerts.get(alerts.size() - 1);
    }
}
