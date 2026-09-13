package br.com.fiap.embarquefacil.data.model;

import java.util.ArrayList;
import java.util.List;

public class Route {
    public String id;
    public String destination;
    public List<RouteStep> steps = new ArrayList<>();
}
