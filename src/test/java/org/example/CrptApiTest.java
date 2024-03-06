package org.example;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;


class CrptApiTest {
    @Test
    void request() throws IOException, URISyntaxException, InterruptedException {
        CrptApi.crptApiURL = "http://localhost:3000";
        CrptApi api = new CrptApi(CrptApi.TimeUnit.FIVESECOND, 2);
        for(int i = 0; i < 10; i++){
            Thread thread = new Thread(() -> {
                System.out.println("thread " + Thread.currentThread().threadId() + " are started");
                try {
                    api.createDocument(Instancio.create(CrptApi.Document.class));
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("thread " + Thread.currentThread().threadId() + " are finished");
            });
            thread.start();
        }
        Thread.sleep(30000);
    }
}