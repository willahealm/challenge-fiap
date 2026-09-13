package br.com.fiap.embarquefacil.ui;

public class UiState<T> {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public final Status status;
    public final T data;
    public final String message;

    private UiState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> UiState<T> idle() { return new UiState<>(Status.IDLE, null, null); }
    public static <T> UiState<T> loading(T existing) { return new UiState<>(Status.LOADING, existing, null); }
    public static <T> UiState<T> success(T data) { return new UiState<>(Status.SUCCESS, data, null); }
    public static <T> UiState<T> error(String message, T existing) { return new UiState<>(Status.ERROR, existing, message); }
}
