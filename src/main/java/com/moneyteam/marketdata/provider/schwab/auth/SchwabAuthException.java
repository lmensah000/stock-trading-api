package com.moneyteam.marketdata.provider.schwab.auth;

/**
 * Raised when the integration cannot authenticate and cannot fix itself.
 *
 * {@code reauthorizationRequired} distinguishes the two cases, because they
 * need different responses: a transient failure should be retried, while an
 * expired or already-spent refresh token needs a human to complete a browser
 * login. Collapsing them into one generic error means the second looks like
 * the first and gets retried pointlessly for days.
 */
public class SchwabAuthException extends RuntimeException {

    private final boolean reauthorizationRequired;

    public SchwabAuthException(String message, boolean reauthorizationRequired) {
        super(message);
        this.reauthorizationRequired = reauthorizationRequired;
    }

    public SchwabAuthException(String message, boolean reauthorizationRequired, Throwable cause) {
        super(message, cause);
        this.reauthorizationRequired = reauthorizationRequired;
    }

    public boolean isReauthorizationRequired() {
        return reauthorizationRequired;
    }
}
