package tasks;

import io.Constants;
import io.MMapFileReader;
import messages.MMapPrimeResponse;

/**
 * PrimeResponseConsumer - Thread to consume responses from "PrimeResponses"
 * memory map file and display repsonse to standard output
 */
public class PrimeResponseConsumer implements Runnable{
    @Override
    public void run() {
        try(MMapFileReader reader =
                    new MMapFileReader(Constants.RESPONSE_MMAP, Constants.FILE_SIZE, Constants.RECORD_SIZE)){
            reader.open();

            while (true) {
                if (reader.next()) {
                    int type = reader.readType();
                    if(type == MMapPrimeResponse.TYPE){
                        MMapPrimeResponse primeResponse = new MMapPrimeResponse();
                        reader.readMessage(primeResponse);
                        System.out.println(primeResponse);
                    }else{
                        throw new RuntimeException("Unknown type: " + type);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
