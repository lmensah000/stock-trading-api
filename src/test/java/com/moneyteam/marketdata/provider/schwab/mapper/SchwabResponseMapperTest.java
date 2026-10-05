package com.moneyteam.marketdata.provider.schwab.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.trading.model.OptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Schwab JSON onto the domain value types. */
class SchwabResponseMapperTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static com.fasterxml.jackson.databind.JsonNode parse(String s) throws Exception {
        return JSON.readTree(s);
    }

    @Test
    @DisplayName("candles map to bars with full decimal precision preserved")
    void mapsCandles() throws Exception {
        // 1709562600000 = 2024-03-04T14:30:00Z
        var root = parse("""
                {"candles":[
                  {"open":187.1501,"high":188.2500,"low":186.9900,"close":188.0100,
                   "volume":1234567,"datetime":1709562600000}
                ]}""");

        List<Bar> bars = SchwabResponseMapper.toBars(root, "AAPL", Timeframe.M5);

        assertThat(bars).hasSize(1);
        Bar bar = bars.get(0);
        // Read through decimalValue, not asDouble: routing through a double
        // would round before it ever reached BigDecimal.
        assertThat(bar.open()).isEqualByComparingTo("187.1501");
        assertThat(bar.close()).isEqualByComparingTo("188.0100");
        assertThat(bar.volume()).isEqualTo(1234567L);
        assertThat(bar.openTime()).isEqualTo(Instant.parse("2024-03-04T14:30:00Z"));
    }

    @Test
    @DisplayName("an empty candle array maps to no bars rather than failing")
    void emptyCandles() throws Exception {
        assertThat(SchwabResponseMapper.toBars(parse("{\"candles\":[]}"), "AAPL", Timeframe.M5)).isEmpty();
        assertThat(SchwabResponseMapper.toBars(parse("{}"), "AAPL", Timeframe.M5)).isEmpty();
    }

    @Test
    @DisplayName("newest-first candles are rejected rather than silently inverting every gap")
    void rejectsReversedCandles() throws Exception {
        var root = parse("""
                {"candles":[
                  {"open":1,"high":2,"low":1,"close":2,"volume":10,"datetime":1709566200000},
                  {"open":1,"high":2,"low":1,"close":2,"volume":10,"datetime":1709562600000}
                ]}""");

        assertThatThrownBy(() -> SchwabResponseMapper.toBars(root, "AAPL", Timeframe.M5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("the option chain maps calls and puts with volume and open interest")
    void mapsOptionChain() throws Exception {
        var root = parse("""
                {
                  "callExpDateMap": {
                    "2024-03-15:11": {
                      "190.0": [{"strikePrice":190.0,"bid":5.10,"ask":5.30,"last":5.20,
                                 "totalVolume":4200,"openInterest":1000}]
                    }
                  },
                  "putExpDateMap": {
                    "2024-03-15:11": {
                      "180.0": [{"strikePrice":180.0,"bid":2.10,"ask":2.20,"last":2.15,
                                 "totalVolume":900,"openInterest":5000}]
                    }
                  }
                }""");

        Instant asOf = Instant.parse("2024-03-04T19:00:00Z");
        Instant oiAsOf = Instant.parse("2024-03-01T21:00:00Z");

        List<OptionQuote> quotes = SchwabResponseMapper.toOptionQuotes(root, "AAPL", asOf, oiAsOf);

        assertThat(quotes).hasSize(2);

        OptionQuote call = quotes.stream()
                .filter(q -> q.optionType() == OptionType.CALL).findFirst().orElseThrow();
        assertThat(call.strike()).isEqualByComparingTo("190.0");
        assertThat(call.expiration()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(call.volume()).isEqualTo(4200);
        assertThat(call.openInterest()).isEqualTo(1000);
        assertThat(call.volumeToOpenInterestRatio()).isEqualByComparingTo("4.2000");

        assertThat(quotes).anyMatch(q -> q.optionType() == OptionType.PUT);
    }

    @Test
    @DisplayName("openInterestAsOf stays distinct from asOf, keeping the staleness guard alive")
    void openInterestTimestampIsSeparate() throws Exception {
        var root = parse("""
                {"callExpDateMap":{"2024-03-15:11":{"190.0":[
                  {"strikePrice":190.0,"totalVolume":100,"openInterest":50}]}}}""");

        Instant asOf = Instant.parse("2024-03-04T19:00:00Z");
        Instant oiAsOf = Instant.parse("2024-03-01T21:00:00Z");

        OptionQuote quote = SchwabResponseMapper.toOptionQuotes(root, "AAPL", asOf, oiAsOf).get(0);

        // If these collapsed to the same instant, VolumeOpenInterestRule's
        // maximum-age check would silently never trigger, and a ratio computed
        // against a days-old denominator would read as live.
        assertThat(quote.asOf()).isEqualTo(asOf);
        assertThat(quote.openInterestAsOf()).isEqualTo(oiAsOf);
        assertThat(quote.openInterestAsOf()).isBefore(quote.asOf());
    }

    @Test
    @DisplayName("the previous session close is the prior weekday at 16:00 ET")
    void previousSessionCloseIsPriorWeekday() {
        // Monday 2024-03-04, 14:00 UTC = 09:00 ET, before the open.
        Instant mondayMorning = Instant.parse("2024-03-04T14:00:00Z");

        Instant close = SchwabResponseMapper.previousSessionClose(mondayMorning);

        // Steps back over the weekend to Friday's close, not Sunday.
        assertThat(close).isEqualTo(Instant.parse("2024-03-01T21:00:00Z"));
    }

    @Test
    @DisplayName("after today's close, the current session's close is used")
    void afterCloseUsesToday() {
        // Monday 22:00 UTC = 17:00 ET, after the 16:00 close.
        Instant mondayEvening = Instant.parse("2024-03-04T22:00:00Z");

        assertThat(SchwabResponseMapper.previousSessionClose(mondayEvening))
                .isEqualTo(Instant.parse("2024-03-04T21:00:00Z"));
    }

    @Test
    @DisplayName("open interest is never stamped in the future relative to the quote")
    void openInterestIsNeverAheadOfTheQuote() {
        Instant now = Instant.parse("2024-03-06T18:30:00Z");

        assertThat(Duration.between(SchwabResponseMapper.previousSessionClose(now), now))
                .isPositive();
    }
}
