package com.moneyteam.marketdata.provider.schwab.client;

/** A failed Schwab market-data call. Carries the status so retry policy can branch on it. */
public class SchwabApiException extends RuntimeException {

    private final int statusCode;

    public SchwabApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SchwabApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** 429 and 5xx are worth retrying; a 4xx generally is not. */
    public boolean isRetryable() {
        return statusCode == 429 || statusCode >= 500;
    }
}
