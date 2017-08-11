import java.io.IOException;
import java.util.*;

/**
 * Created by Marooned on 03/02/2017.
 */

/**
 * The NEAR operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

    /**
     * Evaluate the query operator; the result is an internal inverted
     * list that may be accessed via the internal iterators.
     *
     * @throws IOException Error accessing the Lucene index.
     */
    private int operatorDistance;

    public QryIopNear() {
    }

    public QryIopNear(int operatorDistance) {
        this.operatorDistance = operatorDistance;
    }

    public QryIopNear(int operatorDistance, double weight) {
        this.operatorDistance = operatorDistance;
        this.weight = weight;
    }

    protected void evaluate() throws IOException {

        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.

        this.invertedList = new InvList(this.getField());

        if (args.size() == 0) {
            return;
        }

        //  Each pass of the loop iterate one document that contains all the terms.
        //  If exhausts all the documents that satisify the condition, then evaluate() ends.

        while (this.docIteratorHasMatchAll(null)) {

            // Get the document id that contains all the terms.
            int docid = this.args.get(0).docIteratorGetMatch();
            // Call the funciton withinDoc() to get all the matching locs within one document.
            ArrayList<Integer> positions = withinDoc();
            // If there exists matchings within the document, add them into the inverted list.
            if (positions.size() != 0)
                this.invertedList.appendPosting(docid, positions);
            // Keep moving forward the document
            this.args.get(0).docIteratorAdvancePast(docid);
        }
        // All docids have been processed.  Done.

    }

    protected ArrayList<Integer> withinDoc() {
        ArrayList<Integer> positions = new ArrayList<Integer>();
        while (!outofDoc()) {
            // If we have not exhaust the current document, call findMatch() to find one match within the document
            int position = findMatch();
            if (position != -1) {
                // If the returned value is valid, we add the postion into th result list of current document
                // And we should also forward the loc for each term within the query
                positions.add(position);
                for (Qry arg : this.args) {
                    ((QryIop) arg).locIteratorAdvance();
                }
            }
        }
        return positions;
    }


    // Determine whether we have exhausted the current document.
    protected boolean outofDoc() {
        for (Qry arg : this.args) {
            if (!((QryIop) arg).locIteratorHasMatch())
                return true;
        }
        return false;
    }


    // Find one match within a document.
    protected int findMatch() {
        for (int i = 1; i < this.args.size(); i++) {
            // If the current position, the neighbouring two terms do not match, we return invalid value -1.
            if (((QryIop) this.args.get(i)).locIteratorGetMatch() <= ((QryIop) this.args.get(i - 1)).locIteratorGetMatch()) {
                ((QryIop) this.args.get(i)).locIteratorAdvance();
                return -1;
            }
            if (((QryIop) this.args.get(i)).locIteratorGetMatch() - ((QryIop) this.args.get(i - 1)).locIteratorGetMatch() > this.operatorDistance) {
                ((QryIop) this.args.get(i - 1)).locIteratorAdvance();
                return -1;
            }
        }
        // If we match all the terms, we should return the loc of the last term.
        return ((QryIop) this.args.get(this.args.size() - 1)).locIteratorGetMatch();
    }

}
