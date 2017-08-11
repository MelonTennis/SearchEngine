/**
 * Created by Marooned on 20/02/2017.
 */
public class RetrievalModelIndri extends RetrievalModel {
    public int mu;
    public double lambda;
    public RetrievalModelIndri(int mu, double lambda) {
        this.mu = mu;
        this.lambda = lambda;
    }
    public String defaultQrySopName () {
        return new String ("#and");
    }

}