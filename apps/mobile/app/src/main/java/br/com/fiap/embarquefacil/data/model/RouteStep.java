package br.com.fiap.embarquefacil.data.model;

public class RouteStep {
    public String id;
    public String pointId;
    public String title;
    public String instruction;
    public int distanceMeters;
    public float headingDegrees;

    public RouteStep() {}

    public RouteStep(String id, String title, String instruction, int distanceMeters, float headingDegrees) {
        this.id = id;
        this.pointId = id;
        this.title = title;
        this.instruction = instruction;
        this.distanceMeters = distanceMeters;
        this.headingDegrees = headingDegrees;
    }

    public String displayTitle() {
        if (title != null && !title.isBlank()) return title;
        if (pointId == null) return "Próximo marco";
        if (pointId.contains("sector") || pointId.contains("setor")) return "Setor indicado";
        if (pointId.contains("platform") || pointId.contains("plataforma")) return "Plataforma";
        return "Próximo marco";
    }
}
