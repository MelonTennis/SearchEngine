import java.io.IOException;

/**
 * Created by Marooned on 21/02/2017.
 */
public class QrySopWsum extends QrySop {

    public QrySopWsum() {
    }

    public QrySopWsum(double weight) {
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
        if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    /**
     * Get a score for the document under Inri model.
     *
     * @param r Retrieval model.
     * @return The document score.
     * @throws IOException Error accessing the Lucene index
     */
    public double getScoreIndri(RetrievalModel r) throws IOException {
        double wsum_score = 0;
        int docid = this.docIteratorGetMatch();

        // Sum all the weight for each term within query.
        double sum_weight = 0;
        for (Qry arg : this.args) {
            sum_weight += arg.getWeight();
        }

        // Calculate the WSUM score.
        for (Qry arg : this.args) {
            double arg_score;
            if (arg.docIteratorHasMatchCache() && docid == arg.docIteratorGetMatch()) {
                // If matches, add the actual score.
                arg_score = ((QrySop) arg).getScore(r);
            } else {
                // Else, add the default score.
                arg_score = ((QrySop) arg).getDefaultScore(r, docid);
            }
            wsum_score += arg_score * (arg.getWeight() / sum_weight);
        }
        return wsum_score;
    }

    /**
     * Get the default score for #WSUM operator.
     *
     * @param r     The retrieval model that determines how scores are calculated.
     * @param docid The document ID that does not match.
     * @return The default score for certain retrieval model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getDefaultScoreIndri(r, docid);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WSUM operator.");
        }
    }
    /**
     * Get the default score for #WSUM operator under Indri model.
     *
     * @param r Retrieval model.
     * @param docid The document ID that does not match.
     * @return The default score for Indri model.
     * @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScoreIndri(RetrievalModel r, long docid) throws IOException {

        // Get the total weight.
        double sum_weight = 0;
        for (Qry arg : this.args) {
            sum_weight += arg.getWeight();
        }

        // Calculate the default score for #WSUM operator.
        double wsum_default_score = 0;
        for (Qry arg : this.args) {
            // Combine the default scores of all the arguments.
            wsum_default_score += ((QrySop) arg).getDefaultScore(r, docid) * (arg.getWeight() / sum_weight);
        }
        return wsum_default_score;
    }

}
