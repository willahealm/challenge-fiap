package br.com.fiap.embarquefacil.data.model;

public class Trip {
    public String id;
    public String origin;
    public String destination;
    public String departureAt;
    public String carrier;
    public String company;
    public String platform;
    public String status;
    public String terminal;

    public String carrierName() {
        if (carrier != null && !carrier.isBlank()) return carrier;
        return company == null ? "Viação Cometa" : company;
    }
}
