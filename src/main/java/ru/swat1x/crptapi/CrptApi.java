package ru.swat1x.crptapi;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CrptApi {

  private static final String EXAMPLE_API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

  Gson gson;
  QueriesFrequencyManager frequencyManager;
  CloseableHttpClient httpClient;

  public CrptApi() {
    this.gson = new Gson();
    this.frequencyManager = new QueriesFrequencyManager(TimeUnit.SECONDS, 15);
    this.httpClient = HttpClientBuilder.create()
            .setSSLHostnameVerifier(new NoopHostnameVerifier()) // на случай использования апи без ssl
            .setMaxConnTotal(100)
            .build();
  }

  public void executeCreateDocument(Document document) {
    executeCreateDocument(gson.toJson(document)); // документик в жсон
  }

  public void executeCreateDocument(String documentJson) {
    if (!frequencyManager.trySendQuery()) return; // блокируем выполнение запроса
    sendPostRequest0(EXAMPLE_API_URL, documentJson);
  }

  private void sendPostRequest0(String url, String jsonBody) {
    var postQuery = new HttpPost(url);
    postQuery.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
    try (var response = httpClient.execute(postQuery)) {
      log.debug("Post request to '{}' returns '{}' with code {}",
              url, EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      throw new RuntimeException("exception when making a post request to a url " + url, e);
    }
  }

  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  public static class QueriesFrequencyManager {

    TimeUnit timeUnit;
    int requestLimit;

    List<Long> requests = new LinkedList<>();

    public QueriesFrequencyManager(@NonNull TimeUnit timeUnit, int requestLimit) {
      this.timeUnit = timeUnit;
      if (requestLimit <= 0) throw new IllegalStateException("request limit must be positive");
      this.requestLimit = requestLimit;
    }

    public boolean trySendQuery() {
      var instant = System.currentTimeMillis();

      if (requests.size() < requestLimit + 1) {
        requests.add(0, instant);
        return true;
      } else {
        // определяем порог запросов исходя из тайм юнита
        var border = System.currentTimeMillis() - timeUnit.toMillis(1);

        int recentRequests = 0;
        for (var requestTime : requests) if (border <= requestTime) recentRequests++;
        if (recentRequests >= requestLimit) return false;

        // удаляем последний элемент и кладём первый
        requests.remove(requestLimit - 1);
        requests.add(0, instant);
        return true;
      }
    }

  }

  @Getter
  @Setter
  @Accessors(chain = true)
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Document {

    @SerializedName("description")
    Description description;

    @SerializedName("doc_id")
    String docId;

    @SerializedName("doc_status")
    String docStatus;

    @SerializedName("doc_type")
    String docType;

    @SerializedName("importRequest")
    boolean importRequest;

    @SerializedName("owner_inn")
    String ownerInn;

    @SerializedName("participant_inn")
    String participantInn;

    @SerializedName("producer_inn")
    String producerInn;

    @SerializedName("production_date")
    Date productionDate;

    @SerializedName("production_type")
    String productionType;

    @SerializedName("products")
    List<Product> products;

    @SerializedName("reg_date")
    Date regDate;

    @SerializedName("reg_number")
    String regNumber;

    @Getter
    @Setter
    @Accessors(chain = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Description {

      @SerializedName("participantInn")
      String participantInn;

    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Product {

      @SerializedName("certificate_document")
      String certificateDocument;

      @SerializedName("certificate_document_date")
      Date certificateDocumentDate;

      @SerializedName("certificate_document_number")
      String certificateDocumentNumber;

      @SerializedName("owner_inn")
      String ownerInn;

      @SerializedName("producer_inn")
      String producerInn;

      @SerializedName("production_date")
      Date productionDate;

      @SerializedName("tnved_code")
      Date tnvedCode;

      @SerializedName("uit_code")
      Date uitCode;

      @SerializedName("uitu_code")
      Date uituCode;

    }

  }

}
