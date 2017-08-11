/**
 * Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 * The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

    public QrySopOr() {
    }

    public QrySopOr(double weight) {
        this.weight = weight;
    }

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }


    /**
     * Get a score for the document that docIteratorHasMatch matched.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws IOException Error accessing the Lucene index
     */
    public double getScore(RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean(r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the OR operator.");
        }
    }

    /**
     * getScore for the UnrankedBoolean retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    /**
     * getScore for the RankedBoolean retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            // If does not match, return 0 as score.
            return 0.0;
        } else {
            // If matches, we return the maximum score among all the arguments of this operator.
            double or_score = Double.MIN_VALUE;
            int doc = this.docIteratorGetMatch();
            for (Qry arg : this.args) {
                if (arg.docIteratorHasMatchCache() && doc == arg.docIteratorGetMatch())
                    or_score = Math.max(or_score, ((QrySop) arg).getScore(r));
            }
            return or_score;
        }
    }

    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        return 0.0;
    }

}
