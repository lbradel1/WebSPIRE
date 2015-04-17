/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starspire.models;

/**
 *
 * @author lbradel1
 */
public class EntDocsTF {
    private double TF;
    private Document doc;
    
    public EntDocsTF(double tf, Document d) {
        TF = tf;
        doc = d;
    }
    
    public double getTF() {
        return TF;
    }
    
    public Document getDoc() {
        return doc;
    }
    
    public boolean docEquals(Document d) {
        return(doc.equals(d));
    }
    
}
