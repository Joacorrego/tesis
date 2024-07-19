package com.pruebaapi.app;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

     // inicializar Firebase
     @PostConstruct
     public void initializeFirebase() {
         try {
             // credenciales en JSON
             InputStream serviceAccount = this.getClass().getClassLoader().getResourceAsStream("realbattleships-firebase-adminsdk-4r9w0-28d50e523e.json");
             FirebaseOptions options = new FirebaseOptions.Builder()
                     .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                     .build();

             if (FirebaseApp.getApps().isEmpty()) {
                 FirebaseApp.initializeApp(options);
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 }
