package tasks;

import io.Constants;
import io.MMapFileWriter;
import messages.MMapInteger;

import java.util.Random;

/**
 * Thread populates "PrimeRequests" memory map file for the data to be shared
 * to "PrimeChecker" process.
 */
public class PrimeProducer implements Runnable{
    private int noOfPrimes;

    public PrimeProducer(int n){
        this.noOfPrimes = n;
    }

    @Override
    public void run() {

        try(MMapFileWriter writer =
                    new MMapFileWriter(Constants.REQUESTS_MMAP, Constants.FILE_SIZE, Constants.RECORD_SIZE)){
            writer.open();
            System.out.println("Inside run " + noOfPrimes);
            Random rnd = new Random();
            for (int i = 0; i < noOfPrimes; i++) {
                int data = rnd.nextInt(10000000);
                System.out.println("Sending message " + i + " with data "+ data);
                MMapInteger message = new MMapInteger(data);
                writer.write(message);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
