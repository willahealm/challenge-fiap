package br.com.fiap.embarquefacil.data;

public interface ResultCallback<T> {
    void onSuccess(T value);
    void onError(String message);
}
