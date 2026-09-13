package br.com.fiap.embarquefacil;

import android.app.Application;

import br.com.fiap.embarquefacil.data.AppRepository;
import br.com.fiap.embarquefacil.data.SessionStore;

public class EasyBoardingApp extends Application {
    private SessionStore sessionStore;
    private AppRepository repository;

    @Override public void onCreate() {
        super.onCreate();
        sessionStore = new SessionStore(this);
        repository = new AppRepository(sessionStore);
    }

    public SessionStore sessionStore() { return sessionStore; }
    public AppRepository repository() { return repository; }
}
