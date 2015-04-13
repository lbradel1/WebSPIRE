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
    
    private long init;
    private long count;
    private List<Long> averageStore[];
    private int runs;
    
    public TimeKeeper() {
        init = System.currentTimeMillis();
        runs = 0;
    }
    /**
     * Prepares object to receive averaging commands.
     * Call once immediately before beginning average calculation.
     * It is not necessary to call this function if the constructor has just been 
     * called.
     */
    public void reinitialize()    {
        init = System.currentTimeMillis();
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
     * @param number Number of averages which will be calculated
     */
    public void setAverages(int number) {
        for(int i = 0; i < number; i++) {
            averageStore[i] = new ArrayList<Long>();
        }
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
        averageStore[index].add(temp - init);
        init = temp;
    }
    /**
     * Calculates an average which has been previously initialized.
     * 
     * @param index which average to return
     * @return the average value
     */
    public long getAverage(int index)   {
        long sum = 0;
        for(Long l : averageStore[index])   {
            sum+=l;
        }
        return(sum/averageStore[index].size());
    }
    
    public void printAverages() {
        System.out.println();
        for(int i = 0; i < averageStore.length; i++)    {
            System.out.println(this.getAverage(i));            
        }
    }
}
