import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Marooned on 21/02/2017.
 */
public class QryIopWindow extends QryIop {

    /**
     * Evaluate the query operator; the result is an internal inverted
     * list that may be accessed via the internal iterators.
     *
     * @throws IOException Error accessing the Lucene index.
     */
    private int operatorDistance;

    public QryIopWindow() {
    }

    public QryIopWindow(int operatorDistance) {
        this.operatorDistance = operatorDistance;
    }

    public QryIopWindow(int operatorDistance, double weight) {
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
        //  If exhausts all the documents that satisfy the condition, then evaluate() ends.

        while (this.docIteratorHasMatchAll(null)) {

            // Get the document id that contains all the terms.
            int docid = this.args.get(0).docIteratorGetMatch();
            // Call the function withinDoc() to get all the matching locs within one document.
            ArrayList<Integer> positions = withinDoc();
            // If there exist matches within the document, add them into the inverted list.
            if (positions.size() != 0)
                this.invertedList.appendPosting(docid, positions);
            // Keep moving forward the document
            this.args.get(0).docIteratorAdvancePast(docid);
        }
        // All docs have been processed.  Done.

    }

    protected ArrayList<Integer> withinDoc() {
        ArrayList<Integer> positions = new ArrayList<Integer>();
        while (!outofDoc()) {
            // If we have not exhaust the current document, call findMatch() to find one match within the document
            int position = findMatch();
            if (position != -1) {
                // If the returned value is valid, we add the position into th result list of current document
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
        int min = Integer.MAX_VALUE;
        int min_arg = 0;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < this.args.size(); i++) {
            // Find the maximum and minimum loc locations within all the terms.
            if (((QryIop) this.args.get(i)).locIteratorGetMatch() < min) {
                min = ((QryIop) this.args.get(i)).locIteratorGetMatch();
                min_arg = i;
            }
            max = Math.max(max, ((QryIop) this.args.get(i)).locIteratorGetMatch());
        }
        if (max - min >= this.operatorDistance) {
            // If all the terms don't lie in the window, move forward the smallest loc, return error -1.
            ((QryIop) this.args.get(min_arg)).locIteratorAdvance();
            return -1;
        } else {
            // If there is a match within the doc, return the largest loc to insert in the invList.
            return max;
        }
    }

}

