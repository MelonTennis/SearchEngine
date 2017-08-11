/**
 * Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */


import java.io.*;
import java.lang.IllegalArgumentException;

/**
 * The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

    public QrySopScore() {
    }

    public QrySopScore(double weight) {
        this.weight = weight;
    }

    /**
     *  Document-independent values that should be determined just once.
     *  Some retrieval models have these, some don't.
     */

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchFirst(r);
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
        } else if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25(r);
        } else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SCORE operator.");
        }
    }

    /**
     * getScore for the Unranked retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score under Unranked retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
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
     * @return The document score under Ranked retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
        }
    }

    /**
     * getScore for the BM25 retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score under BM25 retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreBM25(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            String field = ((QryIop) this.args.get(0)).getField();
            double k_1 = ((RetrievalModelBM25) r).k_1;
            double b = ((RetrievalModelBM25) r).b;
            double k_3 = ((RetrievalModelBM25) r).k_3;
            long N = Idx.getNumDocs();
            int tf = ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
            int df = ((QryIop) this.args.get(0)).getDf();
            int qtf = 1;
            long doclen = Idx.getFieldLength(field, this.docIteratorGetMatch());
            float avg_doclen = Idx.getSumOfFieldLengths(field) / (float) Idx.getDocCount(field);
            double RSJ_weight = Math.log((N - df + 0.5) / (df + 0.5));
            double tf_weight = tf / (tf + k_1 * (1 - b + b * (doclen / avg_doclen)));
            double user_weight = (k_3 + 1) * qtf / (k_3 + qtf);
            return RSJ_weight * tf_weight * user_weight;
        }
    }

    /**
     * getScore for the Indri retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score under Indri retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    private double getScoreIndri(RetrievalModel r) throws IOException {
        String field = ((QryIop) this.args.get(0)).getField();
        double mu = ((RetrievalModelIndri) r).mu;
        double lambda = ((RetrievalModelIndri) r).lambda;
        int tf = ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
        int ctf = ((QryIop) this.args.get(0)).getCtf();
        long tokens_length = Idx.getSumOfFieldLengths(field);
        long doclen = Idx.getFieldLength(field, this.docIteratorGetMatch());
        double q_c_MLE = (double) ctf / tokens_length;
        double score = (1 - lambda) * (tf + mu * q_c_MLE) / (doclen + mu) + lambda * q_c_MLE;
        return score;
    }

    /**
     * @param r     The retrieval model that determines how scores are calculated.
     * @param docid The document ID which does not match with the term.
     * @return The default score when doc does not match with the term.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getDefaultScoreIndri(r, docid);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't have default score.");
        }
    }

    /**
     * @param r     The retrieval model that determines how scores are calculated.
     * @param docid The document ID which does not match with the term.
     * @return The default score under Indri model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScoreIndri(RetrievalModel r, long docid) throws IOException {
        String field = ((QryIop) this.args.get(0)).getField();
        double mu = ((RetrievalModelIndri) r).mu;
        double lambda = ((RetrievalModelIndri) r).lambda;
        int tf = 0;
        int ctf = ((QryIop) this.args.get(0)).getCtf();
        long tokens_length = Idx.getSumOfFieldLengths(field);
        long doclen = Idx.getFieldLength(field, (int) docid);
        double q_c_MLE = (double) ctf / tokens_length;
        double score = (1 - lambda) * (tf + mu * q_c_MLE) / (doclen + mu) + lambda * q_c_MLE;
        return score;
    }

    /**
     * Initialize the query operator (and its arguments), including any
     * internal iterators.  If the query operator is of type QryIop, it
     * is fully evaluated, and the results are stored in an internal
     * inverted list that may be accessed via the internal iterator.
     *
     * @param r A retrieval model that guides initialization
     * @throws IOException Error accessing the Lucene index.
     */
    public void initialize(RetrievalModel r) throws IOException {

        Qry q = this.args.get(0);
        q.initialize(r);
    }

    @Override

    /**
     * The weight for score operator should be the score of its first argument (QryIopTerm).
     */
    public double getWeight() {
        return this.args.get(0).getWeight();
    }

}
