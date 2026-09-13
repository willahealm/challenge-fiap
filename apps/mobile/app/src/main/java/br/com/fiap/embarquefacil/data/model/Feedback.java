package br.com.fiap.embarquefacil.data.model;

import java.util.List;

public class Feedback {
    public String id;
    public String journeyId;
    public int rating;
    public List<String> tags;
    public String comment;
    public String createdAt;
}
