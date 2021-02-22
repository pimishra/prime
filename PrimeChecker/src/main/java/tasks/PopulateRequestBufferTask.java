package tasks;

import io.Constants;
import io.MMapFileReader;
import messages.MMapInteger;

import java.util.Queue;

/**
 * PopulateRequestBufferTask - Populates requests queue from the "PrimeRequests"
 * memory map file.
 */
public class PopulateRequestBufferTask implements Runnable{
    private Queue<MMapInteger> requestBuffer;

    public PopulateRequestBufferTask(final Queue<MMapInteger> reqestBuffer){
        this.requestBuffer = reqestBuffer;
    }

    @Override
    public void run() {
        try(MMapFileReader reader =
                    new MMapFileReader(Constants.REQUESTS_MMAP, Constants.FILE_SIZE, Constants.RECORD_SIZE)){
            reader.open();


            while (true) {
                if (reader.next()) {
                    int type = reader.readType();
                    if(type == MMapInteger.TYPE){
                        MMapInteger primeRequest = new MMapInteger();
                        reader.readMessage(primeRequest);
                        requestBuffer.offer(primeRequest);
                    }else {
                        throw new RuntimeException("Unknown type: " + type);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
