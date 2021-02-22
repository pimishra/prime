import io.Constants;
import messages.MMapInteger;
import messages.MMapPrimeResponse;
import tasks.CheckPrimePopulateResponseBufferTask;
import tasks.PopulateRepsonseMMapTask;
import tasks.PopulateRequestBufferTask;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Prime checker runs 3 executor services
 * 1) requestProcessingService - To populate request concurrent queue from "PrimeRequests"
 * memory map file
 * 2) primeProcessingService - To take the request from request queue and calculate if the
 * number is prime and populate response queue
 * 3) responseProcessingService - To take the response from response queue and populate
 * "PrimeResponses" memory map file to be shared back to "Randomizer"
 *
 * The MMapFile interface has been desinged to be MPSC (MultiProducer Single Consumer), hence
 * queues has been introduced to make it MPMC (Multi Producer Multi Consumer). However all queues
 * are java ConcurrentQueue to make it lock free and remove additional overhead.
 *
 * TODO:
 * 1) Make the MMapFile (Memory Map File) as MPMC so that the additional overhead of
 * using ConcurrentQueue can be removed.
 * 2) Even Lock Free Disruptor can be used instead of ConcurrentQueue in the current
 * design to make it higher throughput and lower latency.
 */
public class PrimeCheckerMain {
    public static void main(final String[] args){
        PrimeCheckerMain primeChecker = new PrimeCheckerMain();
        primeChecker.checkPath();
        primeChecker.run();
    }

    private void checkPath() {
        File mmapDir = new File(Constants.MMAP_DIR);
        if(!mmapDir.exists())
            mmapDir.mkdirs();
    }

    private void run() {
        Queue<MMapInteger> requestBuffer = new ConcurrentLinkedQueue<>();
        ExecutorService requestProcessingService = Executors.newFixedThreadPool(1);
        requestProcessingService.submit(
                new PopulateRequestBufferTask(requestBuffer));

        Queue<MMapPrimeResponse> responseBuffer = new ConcurrentLinkedQueue<>();
        int noOfThreads = Runtime.getRuntime().availableProcessors() - 2 > 0?
                Runtime.getRuntime().availableProcessors() - 2 : 1;
        ExecutorService primeProcessingService = Executors.newFixedThreadPool(noOfThreads);
        for(int ind = 0; ind < noOfThreads; ind++){
            primeProcessingService.execute(
                    new CheckPrimePopulateResponseBufferTask(requestBuffer, responseBuffer));
        }

        ExecutorService responseProcessingService = Executors.newFixedThreadPool(1);
        responseProcessingService.execute(
                new PopulateRepsonseMMapTask(responseBuffer));

        requestProcessingService.shutdown();
        primeProcessingService.shutdown();
        responseProcessingService.shutdown();
    }
}
