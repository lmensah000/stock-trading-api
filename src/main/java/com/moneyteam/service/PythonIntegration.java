package com.moneyteam.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PythonIntegration {
    public static void main(String[] args) {
        try {
            String tickerSymbol = "AAPL"; // Example ticker
            ProcessBuilder pb = new ProcessBuilder("python", "stock_data_api.py", tickerSymbol);
            Process process = pb.start();

            // Read the script output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}