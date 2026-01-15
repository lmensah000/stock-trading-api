package com.moneyteam.service;

import com.moneyteam.model.Stock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public abstract class StockApiService {
    private HttpClient httpClient = HttpClient.newHttpClient();

//    public void executeTrade(TradeRequest tradeRequest) {
//        HttpRequest request = HttpRequest.newBuilder()
//            .uri(URI.create("https://api.stocktrading.com/trade"))
//            .header("Content-Type", "application/json")
//            .POST(HttpRequest.BodyPublishers.ofString(tradeRequest.toJson()))
//            .build();
//
//        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    // Process the response, handle errors, etc.
    // Method to retrieve stock details from an external API
    public Stock getStockDetails(String stockTicker) {
        // Initialize HttpClient
        HttpClient httpClient = HttpClient.newHttpClient();

        // Construct the URL for the API request
        String apiUrl = "https://api.example.com/stocks/" + stockTicker;

        // Create an HTTP request

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://foo.com/"))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();

        try {
            // Send the HTTP request and retrieve the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (status code 200)
            if (response.statusCode() == 200) {
                // Parse the response JSON to extract stock details
                String responseBody = response.body();
                // Parse JSON and extract relevant stock details (you would need a JSON parsing library for this)
                // Sample code:
                String stockName = ""; // Extract stockName from JSON
                String sector = ""; // Extract sector from JSON
                double marketCapAmount = 0.0; // Extract market cap from JSON
                int volume = 0;
                double sizzleIndex = 0.0;
                double ask =0;
                double bid = 0;
                int numberOfShares = 0;
                double open =0; 
                double close = 0;
                double last = 0;
                double markChange = 0;
                // Extract other details similarly

                // Create and return a new Stock object with retrieved details
                return new Stock( stockTicker,  stockName,  sector, marketCapAmount,  volume,  sizzleIndex,
                         ask,  bid,  numberOfShares,  open,  close,  last,  markChange);
            } else {
                // Handle error response
                System.out.println("Error: " + response.statusCode());
                return null; // Or throw an exception
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null; // Or throw an exception
        }
    }

    public abstract Optional<Stock> fetchStockDetails(String stockTicker);

    // You can add more methods to fetch additional stock information or perform other operations
}
/*Interacting with an API involves making HTTP requests to the API endpoints and processing the responses.
Here's an example of making an HTTP request to an API using the HttpClient class in Java:

 */

/* In the above example, we use the HttpClient to construct an HTTP request with the desired headers and body.
We then send the request to the API endpoint and receive the response. You can process the response data,
handle errors, and take appropriate actions based on the API's specifications.

Remember, the examples provided are simplified and may need to be adapted to fit your specific requirements and the
APIs you are using. It's important to refer to the documentation of the users service, stock trading API, and
any other relevant APIs for more detailed information.

 */


