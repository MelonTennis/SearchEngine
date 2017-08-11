import java.io.IOException;

/**
 * Created by Marooned on 20/02/2017.
 */
public class QrySopSum extends QrySop {

    public QrySopSum() {
    }

    public QrySopSum(double weight) {
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
        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    /**
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score under BM25 model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getScoreBM25(RetrievalModel r) throws IOException {
        double sum_score = 0;
        int doc = this.docIteratorGetMatch();
        // Sum all the scores for each term within query.
        for (Qry arg : this.args) {
            if (arg.docIteratorHasMatchCache() && doc == arg.docIteratorGetMatch())
                sum_score += ((QrySop) arg).getScore(r);
        }
        return sum_score;
    }


    /**
     * Default score under BM25 model is useless and #SUM operator is only under BM25, so just set it to 0.0.
     */
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        return 0.0;
    }

}
