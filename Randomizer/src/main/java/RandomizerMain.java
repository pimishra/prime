import io.Constants;
import tasks.PrimeProducer;
import tasks.PrimeResponseConsumer;

import java.io.File;

/**
 * Randomizer - Starts two threads
 * 1) PrimeProducer - To relay prime numbers to "PrimeRequests" memory map file
 * 2) PrimeResponseConsumer - To read "PrimeResponses" memory map file and display repsonse
 * to standard output
 */
public class RandomizerMain {
    private static void printHelp(){
        System.out.println(
                "Command format is -- java -jar <jar-file-name> -n <number of primes>");
    }

    public static void main(final String[] args){
        if (args.length != 2){
            printHelp();
            System.exit(-1);
        }
        String option = args[0];
        if(!option.equalsIgnoreCase("-n")){
            printHelp();
            System.exit(-1);
        }
        String value = args[1];
        try {
            int n = Integer.parseInt(value);
            RandomizerMain randomizer = new RandomizerMain();
            randomizer.checkPath();
            randomizer.run(n);
        }catch (Exception e){
            printHelp();
            System.exit(-1);
        }
    }

    private void checkPath() {
        File mmapDir = new File(Constants.MMAP_DIR);
        if(!mmapDir.exists())
            mmapDir.mkdirs();
    }

    private void run(int n) throws InterruptedException {
        Thread producer = new Thread(new PrimeProducer(n));
        producer.start();

        Thread consumer = new Thread(new PrimeResponseConsumer());
        consumer.start();

        producer.join();
        consumer.join();
    }
}
