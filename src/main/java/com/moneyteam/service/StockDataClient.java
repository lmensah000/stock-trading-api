package com.moneyteam.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StockDataClient {

    private static final String API_URL = "http://127.0.0.1:5000/api/stock/";

    public static void fetchStockData(String ticker) {
        String endpoint = API_URL + ticker;

        // Create an HTTP client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create an HTTP GET request
            HttpGet request = new HttpGet(endpoint);

            // Execute the request and handle the response using a lambda
            httpClient.execute(request, (ClassicHttpResponse response) -> {
                int statusCode = response.getCode();

                if (statusCode == 200) {
                    // Parse the response as JSON
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String jsonResponse = EntityUtils.toString(entity);
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(jsonResponse);

                        // Save the JSON response to a file
                        saveJsonToFile(ticker, rootNode);

                        // Print the response to the console
                        System.out.println("Stock Data Retrieved Successfully:");
                        System.out.println(rootNode.toPrettyString());
                    }
                } else {
                    HttpEntity entity = response.getEntity();
                    String responseContent = entity != null ? EntityUtils.toString(entity) : "No content";
                    System.out.println("Failed to fetch stock data. HTTP Status Code: " + statusCode);
                    System.out.println("Response: " + responseContent);
                }
                return null; // Lambda requires a return value, so we return null
            });
        } catch (IOException e) {
            System.out.println("An error occurred while fetching stock data: " + e.getMessage());
        }
    }

    private static void saveJsonToFile(String ticker, JsonNode jsonNode) {
        String fileName = ticker + "_data.json";
        try (FileWriter fileWriter = new FileWriter(new File(fileName))) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileWriter, jsonNode);
            System.out.println("JSON data saved to: " + fileName);
        } catch (IOException e) {
            System.out.println("Error writing JSON to file: " + e.getMessage());
        }
    }

//    public static void main(String[] args) {
//        // Get ticker input from the users
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Enter stock ticker: ");
//        String ticker = scanner.nextLine();
//        fetchStockData(ticker);
//    }
}

/*

HTTP Request with Apache HttpClient:

The HttpGet object is used to send a GET request to the Flask API endpoint.
The response is checked for a 200 OK status to confirm success.
Parsing the JSON Response:

The response body is parsed into a JsonNode using Jackson's ObjectMapper.
This makes it easy to handle the JSON data programmatically.
Saving JSON Data to a File:

The saveJsonToFile method writes the JSON data to a file named <ticker>_data.json, mimicking the Python script's behavior.
Error Handling:

Handles exceptions like IOException and ParseException.
Logs errors if the request fails or the data can't be parsed.

 */