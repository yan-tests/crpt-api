import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final String apiUrl;
    private final TimeUnit timeUnit;
    private final int requestLimit;

    private final Queue<Long> timestampQueue = new ConcurrentLinkedQueue<>();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void createDocument(Document document, String signature) {
        try {
            synchronized (this) {
                do {
                    long currentTime = System.currentTimeMillis();
                    if (!timestampQueue.isEmpty()) {
                        if ((currentTime - timestampQueue.peek() > timeUnit.toMillis(1))) {
                            timestampQueue.poll();
                        } else {
                            wait(100);
                        }
                    }
                } while(timestampQueue.size() == requestLimit);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .build();

            timestampQueue.offer(System.currentTimeMillis());

            HttpResponse response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Document successfully created!");
            } else {
                System.out.println("Failed to create the document. Status code: " + response.statusCode());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public record Description(String participantInn)
    { }

    public record Product(
            @JsonProperty("certificate_document")
            String certificateDocument,

            @JsonProperty("certificate_document_date")
            String certificateDocumentDate,

            @JsonProperty("certificate_document_number")
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
    ) { }

    public record Document(
            Description description,

            @JsonProperty("doc_id")
            String docId,

            @JsonProperty("doc_status")
            String docStatus,

            @JsonProperty("doc_type")
            String docType,

            Boolean importRequest,

            @JsonProperty("owner_inn")
            String ownerInn,

            @JsonProperty("participant_inn")
            String participantInn,

            @JsonProperty("producer_inn")
            String producerInn,

            @JsonProperty("production_date")
            String productionDate,

            @JsonProperty("production_type")
            String productionType,

            ArrayList<Product> products,

            @JsonProperty("reg_date")
            String regDate,

            @JsonProperty("reg_number")
            String regNumber
    ) { }
}