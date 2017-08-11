/**
 * Created by Marooned on 20/02/2017.
 */
public class RetrievalModelBM25 extends RetrievalModel {
    public double k_1;
    public double b;
    public double k_3;

    public RetrievalModelBM25(double k_1, double b, double k_3) {
        this.k_1 = k_1;
        this.b = b;
        this.k_3 = k_3;
    }
    public String defaultQrySopName () {
        return new String ("#sum");
    }

}
