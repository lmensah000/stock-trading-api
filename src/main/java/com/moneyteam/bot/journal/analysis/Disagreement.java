package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextFidelity;
import com.moneyteam.bot.journal.Judgement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One comparison between what the trader did and what the rules said.
 *
 * @param journalEntryId the fill this concerns
 * @param underlying     instrument
 * @param filledAt       when it filled
 * @param verdict        where behaviour and rules agreed or diverged
 * @param firedRules     names of the rules that fired on the replayed snapshot
 * @param fidelity       which tier this observation came from
 * @param judgement      the trader's own verdict, if recorded
 * @param rMultiple      the outcome, if the trade has closed
 */
public record Disagreement(
        Long journalEntryId,
        String underlying,
        Instant filledAt,
        Verdict verdict,
        List<String> firedRules,
        ContextFidelity fidelity,
        Judgement judgement,
        BigDecimal rMultiple) {

    /**
     * Where behaviour and the encoded rules stand relative to each other.
     *
     * The middle case is the whole point of the journal. It is the set of
     * trades taken for reasons the rules do not contain - which is exactly the
     * knowledge that was never written down.
     */
    public enum Verdict {

        /**
         * Taken, and the rules fired. Confirms something already encoded, and
         * is the least informative outcome.
         */
        AGREED,

        /**
         * <strong>Taken, and the rules said nothing.</strong> The articulation
         * gap. Something on the chart justified the trade and no rule
         * describes it.
         */
        YOU_TOOK_RULES_SILENT,

        /**
         * The rules fired and the trade was passed over. Usually an unstated
         * veto - a disqualifier that is real but unrecorded.
         *
         * Only observable for setups the rules already flag, so it is a
         * partial window onto what gets correctly avoided, not a full one. The
         * journal holds no record of moments nobody looked at.
         */
        RULES_FIRED_YOU_PASSED,

        /**
         * The snapshot could not be replayed - absent, or too thin to evaluate.
         * Reported rather than dropped: a quietly skipped entry would make the
         * other counts look like they covered everything.
         */
        NOT_EVALUABLE
    }

    public ContextFidelity contextFidelity() {
        return fidelity;
    }
}
