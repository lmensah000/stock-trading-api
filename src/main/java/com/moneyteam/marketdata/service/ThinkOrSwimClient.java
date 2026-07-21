package com.moneyteam.marketdata.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ThinkOrSwimClient {
/*
This class will:
    Handle authentication.
    Perform API requests.
    Return parsed responses (e.g., Java objects).
 */
private static final String BASE_URL = "https://api.tdameritrade.com/v1";
    private static final String TOKEN_URL = "https://api.tdameritrade.com/v1/oauth2/token";
    private static final String CLIENT_ID = "your_client_id@AMER.OAUTHAP"; // Replace with your client ID
    private static final String REDIRECT_URI = "http://localhost"; // Replace with your redirect URI
    private String accessToken;

    public ThinkOrSwimClient() {
        // Initialize access token if needed
        this.accessToken = null;
    }
    /*
    Open this URL in a web browser. After logging in and authorizing the application, you will be redirected to the specified redirect URI with an authorization code in the URL.
     */
    //https://api.schwabapi.com/v1/oauth/authorize?response_type=code&client_id=YOUR_CLIENT_ID&scope=readonly&redirect_uri=https://developer.schwab.com/oauth2-redirect.html
    //Token URL: https://api.schwabapi.com/v1/oauth/token
    //Flow: authorizationCode
    // Authenticate and fetch access token
    public void authenticate(String authCode) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TOKEN_URL);
            String body = String.format(
                    "grant_type=authorization_code&access_type=offline&code=%s&client_id=%s&redirect_uri=%s",
                    authCode, CLIENT_ID, REDIRECT_URI
            );

            post.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(body));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            String response = EntityUtils.toString(httpClient.execute(post).getEntity());
            JsonNode jsonNode = new ObjectMapper().readTree(response);

            if (jsonNode.has("access_token")) {
                this.accessToken = jsonNode.get("access_token").asText();
                System.out.println("Authenticated successfully! Access Token: " + accessToken);
            } else {
                System.out.println("Authentication failed: " + response);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    // Fetch stock details
    public JsonNode fetchStockDetails(String ticker) throws IOException{
        // Perform API call and return Stock object
        if (this.accessToken == null) {
            throw new IllegalStateException("Client not authenticated. Please call authenticate() first.");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(BASE_URL + "/marketdata/" + ticker + "/quotes");
            get.setHeader("Authorization", "Bearer " + accessToken);

            String response = EntityUtils.toString(httpClient.execute(get).getEntity());
            return new ObjectMapper().readTree(response);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Output filtered data to terminal
    public void filterAndOutputStockData(String ticker) throws IOException {
        JsonNode stockData = fetchStockDetails(ticker);

        if (stockData != null) {
            System.out.println("Stock Details for: " + ticker);
            System.out.println(stockData.toPrettyString());

            // Apply custom filters here (e.g., price > X, volume < Y)
            double price = stockData.get(ticker).get("lastPrice").asDouble();
            if (price > 100) {
                System.out.println("Stock is over $100!");
            }
        } else {
            System.out.println("No data retrieved for ticker: " + ticker);
        }
    }

}
//Handle Token Expiration
//Access tokens may expire, so you will need to implement a mechanism to refresh the
// token using the refresh token you received during the token exchange step.