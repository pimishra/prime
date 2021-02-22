package tasks;

import messages.MMapInteger;
import messages.MMapPrimeResponse;

import java.util.Queue;

/**
 * CheckPrimePopulateResponseBufferTask class reads requests from the input queue,
 * checks if the number is prime and populates response to the response queue
 */
public class CheckPrimePopulateResponseBufferTask implements Runnable {
    private Queue<MMapInteger> requestBuffer;
    private Queue<MMapPrimeResponse> responseBuffer;

    public CheckPrimePopulateResponseBufferTask(final Queue<MMapInteger> requestBuffer,
                                                final Queue<MMapPrimeResponse> responseBuffer){
        this.requestBuffer = requestBuffer;
        this.responseBuffer = responseBuffer;
    }

    @Override
    public void run() {
        while (true){
            final MMapInteger request = requestBuffer.poll();
            if (request != null) {
                final boolean bPrime = isPrime(request.getData());
                final MMapPrimeResponse response = new MMapPrimeResponse(request.getData(), bPrime);
                responseBuffer.offer(response);
            }
        }
    }

    /**
     * Method to check if the input number is prime.
     * @param number
     * @return
     */
    private boolean isPrime(int number)
    {
        int root = (int)Math.sqrt(number) ;
        for(int i=2; i<=root; i++)
        {
            if(number%i == 0)
                return false;
        }
        return true;
    }
}
