package tasks;

import io.Constants;
import io.MMapFileWriter;
import messages.MMapPrimeResponse;

import java.util.Queue;

/**
 * PopulateRepsonseMMapTask - Populates the response from the response queue
 * to the "PrimeResponse" memory map file
 */
public class PopulateRepsonseMMapTask implements Runnable{
    private Queue<MMapPrimeResponse> responseBuffer;

    public PopulateRepsonseMMapTask(final Queue<MMapPrimeResponse> responseBuffer){
        this.responseBuffer = responseBuffer;
    }

    @Override
    public void run() {
        try (MMapFileWriter writer =
                    new MMapFileWriter(Constants.RESPONSE_MMAP, Constants.FILE_SIZE, Constants.RECORD_SIZE)){
            writer.open();

            while(true){
                MMapPrimeResponse response = responseBuffer.poll();
                if(response != null)
                    writer.write(response);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
