package org.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

public class CrptApi {
    public static String crptApiURL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final int requestLimit;
    private final long timeUnit;
    private volatile long timestamp = System.currentTimeMillis();
    private volatile int currentConnectionCount = 0;
    public CrptApi(TimeUnit timeUnit, int requestLimit){
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit.modifier;
    }

    public void createDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
        String payload = getJSON(document);
        send(payload);
    }

    private void send(String payload) throws URISyntaxException, IOException, InterruptedException {
        if(currentConnectionCount + 1 <= requestLimit){
            synchronized (this){
                if(currentConnectionCount + 1 <= requestLimit){
                    currentConnectionCount++;
                }
            }
            HttpRequest request = HttpRequest.newBuilder(new URI(crptApiURL))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } else {
            while(currentConnectionCount + 1 > requestLimit){
                if(System.currentTimeMillis() - timestamp >= timeUnit){
                    synchronized (this){
                        if(System.currentTimeMillis() - timestamp >= timeUnit) {
                            timestamp = System.currentTimeMillis();
                            currentConnectionCount = 0;
                        }
                    }
                }
            }
            send(payload);
        }
    }

    private String getJSON(Document document) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, document);
        return writer.toString();
    }

    @JsonAutoDetect
    public record Document(
            @JsonProperty("description")
            Description description,
            @JsonProperty("doc_id")
            String id,
            @JsonProperty("doc_status")
            String status,
            @JsonProperty("doc_type")
            String type,
            @JsonProperty("importRequest")
            boolean importRequest,
            @JsonProperty("owner_inn")
            String ownerInn,
            @JsonProperty("participant_inn")
            String participantInn,
            @JsonProperty("producer_inn")
            String producerInn,
            @JsonProperty("production_date")
            @JsonDeserialize(using = LocalDateDeserializer.class)
            @JsonSerialize(using = LocalDateSerializer.class)
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate productionDate,
            @JsonProperty("production_type")
            String productionType,
            @JsonProperty("products")
            List<Product> products,
            @JsonProperty("reg_date")
            String regDate,
            @JsonProperty("reg_number")
            String regNumber

    ){
        @JsonAutoDetect
        record Description(
                @JsonProperty("participantInn")
                String participantInn
        ){}
    }
    @JsonAutoDetect
    record Product(
            @JsonProperty("certificate_document")
            String certificateDocument,
            @JsonProperty("certificate_date")
            @JsonFormat(pattern="yyyy-MM-dd")
            @JsonDeserialize(using = LocalDateDeserializer.class)
            @JsonSerialize(using = LocalDateSerializer.class)
            LocalDate certificateDocumentDate,
            @JsonProperty("certificate_number")
            String certificateDocumentNumber,
            @JsonProperty("owner_inn")
            String ownerInn,
            @JsonProperty("producer_inn")
            String producerInn,
            @JsonProperty("production_date")
            String productionDate,
            @JsonProperty("tnved_code")
            String tnvedCode,
            @JsonProperty("uit_code")
            String uitCode,
            @JsonProperty("uitu_code")
            String uituCode
    ){}

    public static class TimeUnit{
        static TimeUnit SECOND = new TimeUnit(1000);
        static TimeUnit FIVESECOND = new TimeUnit(5000);
        static TimeUnit MINUTE = new TimeUnit(1000 * 60);
        static TimeUnit HOUR = new TimeUnit(1000 * 60 * 60);

        final long modifier;
        TimeUnit(long modifier){
            this.modifier = modifier;
        }
    }
}