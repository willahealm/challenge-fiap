package br.com.fiap.embarquefacil.data.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Journey {
    public String id;
    public String tripId;
    public String userId;
    public String stage;
    public String currentPointId;
    public String currentPointName;
    public Map<String, Boolean> checklist = new LinkedHashMap<>();
}
