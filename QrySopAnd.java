/**
 * Created by Marooned on 03/02/2017.
 */

import java.io.*;

/**
 * The OR operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

    public QrySopAnd() {
    }

    public QrySopAnd(double weight) {
        this.weight = weight;
    }

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
            // For UnrankedBoolean and RankedBoolean model, a doc matches only when it matches all the terms.
            return this.docIteratorHasMatchAll(r);
        } else if (r instanceof RetrievalModelIndri) {
            // For Indri model, a doc matches when it at least matches one term.
            return this.docIteratorHasMatchMin(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
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
        } else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
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
            // If the document doesn't match, we return score as 0.
            return 0.0;
        } else {
            // If matches, we return the minimum score among all the arguments of this operator.
            double and_score = Double.MAX_VALUE;
            for (Qry arg : this.args) {
                and_score = Math.min(and_score, ((QrySop) arg).getScore(r));
            }
            return and_score;
        }
    }

    /**
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreIndri(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            // If the document doesn't match, return score as 0.
            return 0.0;
        } else {
            // If matches, return the product of scores of all the arguments.
            double and_score = 1;
            int docid = this.docIteratorGetMatch();

            for (Qry arg : this.args) {

                double arg_score;

                if (arg.docIteratorHasMatchCache() && docid == arg.docIteratorGetMatch()) {
                    // If the argument has match, return the score.
                    arg_score = ((QrySop) arg).getScore(r);
                } else {
                    // If the argument does not match, return the default score.
                    arg_score = ((QrySop) arg).getDefaultScore(r, docid);
                }

                and_score *= Math.pow(arg_score, 1.0 / args.size());
            }

            return and_score;
        }
    }

    /**
     * @param r     The retrieval model that determines how scores are calculated.
     * @param docid The document ID.
     * @return The default score for this operator under certain retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getDefaultScoreIndri(r, docid);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
    }


    /**
     * @param r     The retrieval model that determines how scores are calculated.
     * @param docid The document ID.
     * @return The combination of the default scores of all the arguments under Indri model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScoreIndri(RetrievalModel r, long docid) throws IOException {
        double and_default_score = 1;
        for (Qry arg : this.args) {
            and_default_score *= Math.pow(((QrySop) arg).getDefaultScore(r, docid), 1.0 / this.args.size());
        }
        return and_default_score;
    }

}

