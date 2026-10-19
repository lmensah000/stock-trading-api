package com.moneyteam.bot.journal;

/**
 * The trader's own verdict on a decision, recorded separately from its outcome.
 *
 * This separation is the single most important thing in the journal. A winning
 * trade can be a bad decision and a loser can be a good one, so a system fitted
 * to profit learns to chase luck rather than to recognise a setup. Keeping the
 * verdict and the R outcome as two independent fields means a model can be
 * fitted to either, and the disagreement between them is itself informative -
 * trades that won against your own judgement are the ones to be suspicious of.
 */
public enum Judgement {

    /** Given the same picture again, I would take it again. */
    WOULD_REPEAT,

    /** I would not. Includes trades that happened to win. */
    WOULD_NOT_REPEAT,

    /** Genuinely undecided. Kept distinct from unjudged, which is null. */
    UNSURE
}
