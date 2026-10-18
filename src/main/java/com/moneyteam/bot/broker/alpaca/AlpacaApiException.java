package com.moneyteam.bot.broker.alpaca;

/**
 * An Alpaca call failed.
 *
 * Carries the HTTP status because the retry policy keys off it: a 429 or a 5xx
 * is worth retrying, a 401 or a 422 never is. Retrying a rejected order is not
 * harmless - it is how one intended position becomes several.
 */
public class AlpacaApiException extends RuntimeException {

    private final int statusCode;

    public AlpacaApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AlpacaApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    /**
     * True for failures a retry might clear.
     *
     * 429 and 5xx only. A 401 means the key pair does not match the host, which
     * no number of retries fixes, and a 422 means the order itself was refused.
     */
    public boolean isRetryable() {
        return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    /**
     * True when this looks like paper credentials sent to the live host or the
     * reverse - the documented failure mode, since the two environments have
     * separate key pairs and a key is rejected by the wrong one.
     */
    public boolean isCredentialHostMismatch() {
        return statusCode == 401 || statusCode == 403;
    }
}
