package starspire;

import java.util.ArrayList;
import java.util.List;

/**
 *Contains methods used for measuring the time/average time taken for particular
 * blocks of code.
 * 
 * @author Nathan Wycoff
 */
public class TimeKeeper {
    
    private List<Long> init;
    private long count;
    private ArrayList<ArrayList<Long>> averageStore;
    private int runs;
    
    public TimeKeeper() {
        averageStore = new ArrayList<ArrayList<Long>>();
        runs = 0;
        init = new ArrayList<Long>();
        
        
    }
    
    
    /**
     * Starts time calculation
     */
    public void startStopwatch() {
        count = System.currentTimeMillis();
    }
    
    /**
     * Get time ellapsed since startStopwatch was called
     * @return 
     */
    public long endStopwatch()  {
        return(System.currentTimeMillis()-count);
    }
    
    public void printStopwatch()    {
        System.out.println();
        System.out.println("Run " + runs + " time: " + this.endStopwatch());
        runs++;
    }
    
    /**
     * Prepare to record average times.
     * @param index which average to initialize
     */
    public void setAverages(int index) {
           averageStore.add(index, new ArrayList<Long>());
           init.add(System.currentTimeMillis());
        
    }
    /**
     * This function is called when an iteration of what to be averaged has 
     * occurred, in order to timestamp it.
     * 
     * 1 or more averages must have been initialized through the set averages 
     * function.
     * 
     * @param index which average to stamp
     */
    public void storeAverage(int index) {
        
        long temp = System.currentTimeMillis();
        averageStore.get(index).add(temp - init.get(index));
        init.set(index, temp);
        
    }
    /**
     * Calculates an average which has been previously initialized.
     * 
     * @param index which average to return
     * @return the average value
     */
    public long getAverage(int index)   {
        long sum = 0;
        for(Long l : averageStore.get(index))   {
            sum+=l;
        }
        return(sum/averageStore.get(index).size());
    }
    
    /**
     *Prints value of all averages to the out stream
     */
    public void printAverages() {
        System.out.println();
        for(int i = 0; i < averageStore.size(); i++)    {
            System.out.println("This is an average: " + this.getAverage(i));            
        }
    }
    public void reinitializeAverage(int index)  {
        init.set(index, System.currentTimeMillis());
    }
}
