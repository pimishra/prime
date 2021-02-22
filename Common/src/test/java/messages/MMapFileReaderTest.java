package messages;

import io.MMapFile;
import io.MMapFileReader;
import io.MMapFileWriter;
import io.IMMapMessage;
import org.junit.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.EOFException;
import java.io.File;

import io.Constants.Length;
import io.Constants.StatusFlag;
import io.Constants.Structure;

public class MMapFileReaderTest {
    public static final long FILE_SIZE = 10000;
    public static final int RECORD_SIZE = 12;

    @Test
    public void testReadEmptyFile() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest1";
        new File(fileName).delete();
        try(MMapFileReader reader = new MMapFileReader(fileName, FILE_SIZE, RECORD_SIZE)){
            reader.open();
            assertEquals(false, reader.next());
        }
        new File(fileName).delete();
    }

    @Test(expected= EOFException.class)
    public void testReadEOF() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest2";
        new File(fileName).delete();

        int fileSize = Length.Limit + Length.RecordHeader + RECORD_SIZE;
        byte[] data = new byte[RECORD_SIZE];
        try(MMapFileWriter writer = new MMapFileWriter(fileName, fileSize, RECORD_SIZE)){
            writer.open();
            writer.write(data, 0, data.length);
        }

        try(MMapFileReader reader = new MMapFileReader(fileName, fileSize, RECORD_SIZE)){
            reader.open();
            assertEquals(true, reader.next());
            assertEquals(RECORD_SIZE, reader.readBuffer(data, 0));
            reader.next(); // throws EOFException
        }

        new File(fileName).delete();
    }

    @Test
    public void testReadBuffer() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest3";
        new File(fileName).delete();

        byte[] data1 = {0, 1, 2, 3};
        byte[] data2 = {4, 5, 6};
        try(MMapFileWriter writer = new MMapFileWriter(fileName, FILE_SIZE, RECORD_SIZE)){
            writer.open();
            writer.write(data1, 0, data1.length);
            writer.write(data2, 0, data2.length);
        }

        try(MMapFileReader reader = new MMapFileReader(fileName, FILE_SIZE, RECORD_SIZE)){
            reader.open();

            byte[] buffer = new byte[4];
            assertEquals(true, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(4, reader.readBuffer(buffer, 0));
            assertArrayEquals(data1, buffer);

            buffer = new byte[3];
            assertEquals(true, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(3, reader.readBuffer(buffer, 0));
            assertArrayEquals(data2, buffer);

            assertEquals(false, reader.next());
            assertEquals(true, reader.hasRecovered());
        }

        new File(fileName).delete();
    }

    @Test
    public void testReadMessage() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest4";
        new File(fileName).delete();

        try(MMapFileWriter writer = new MMapFileWriter(fileName, FILE_SIZE, RECORD_SIZE)){
            writer.open();

            PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
            writer.write(priceUpdate);

            priceUpdate = new PriceUpdate(3, 4, 5);
            writer.write(priceUpdate);
        }

        try(MMapFileReader reader = new MMapFileReader(fileName, FILE_SIZE, RECORD_SIZE)){
            reader.open();

            PriceUpdate priceUpdate = new PriceUpdate();

            assertEquals(true, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(0, reader.readType());
            reader.readMessage(priceUpdate);
            assertEquals(0, priceUpdate.getSource());
            assertEquals(1, priceUpdate.getPrice());
            assertEquals(2, priceUpdate.getQuantity());

            assertEquals(true, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(0, reader.readType());
            reader.readMessage(priceUpdate);
            assertEquals(3, priceUpdate.getSource());
            assertEquals(4, priceUpdate.getPrice());
            assertEquals(5, priceUpdate.getQuantity());

            assertEquals(false, reader.next());
            assertEquals(true, reader.hasRecovered());
        }

        new File(fileName).delete();
    }

    @Test
    public void testCrashBeforeCommitRollbackBySameReader() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest5";
        new File(fileName).delete();

        MMapFileWriter writer = new MMapFileWriter(fileName, FILE_SIZE, RECORD_SIZE);
        writer.open();

        // write first record
        PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
        writer.write(priceUpdate);

        // write second record
        priceUpdate = new PriceUpdate(3, 4, 5);
        writer.write(priceUpdate);

        writer.close();

        // set commit flag to false for the first record
        MMapFile mem = new MMapFile(fileName, FILE_SIZE);
        mem.putIntVolatile(Structure.Data, 0);

        MMapFileReader reader = new MMapFileReader(fileName, FILE_SIZE, RECORD_SIZE);
        reader.setTimeout(0);
        reader.open();

        assertEquals(0, reader.getTimeoutCounter());
        assertEquals(0, reader.getTimerStart());
        for (int i = 0; i < MMapFileReader.MAX_TIMEOUT_COUNT - 1; i++) {
            assertEquals(false, reader.next());
        }
        assertEquals(99, reader.getTimeoutCounter());
        assertEquals(0, reader.getTimerStart());

        // the reader starts the timer
        assertEquals(false, reader.next());
        assertEquals(false, reader.hasRecovered());
        assertEquals(100, reader.getTimeoutCounter());
        assertTrue(reader.getTimerStart() > 0);

        // the reader sets the roll back flag and skips the record
        assertEquals(false, reader.next());
        assertEquals(false, reader.hasRecovered());
        assertEquals(0, reader.getTimeoutCounter());
        assertEquals(0, reader.getTimerStart());

        // the reader reads the second record
        assertEquals(true, reader.next());
        assertEquals(false, reader.hasRecovered());
        assertEquals(0, reader.readType());
        reader.readMessage(priceUpdate);
        assertEquals(3, priceUpdate.getSource());
        assertEquals(4, priceUpdate.getPrice());
        assertEquals(5, priceUpdate.getQuantity());

        // no more records available
        assertEquals(false, reader.next());
        assertEquals(true, reader.hasRecovered());

        reader.close();

        new File(fileName).delete();
    }

    @Test
    public void testCrashBeforeCommitRollbackByDifferentReaderBefore() throws Exception {
        String fileName = "C:/Temp/MMapReaderTest6";
        new File(fileName).delete();

        try(MMapFileWriter writer = new MMapFileWriter(fileName, FILE_SIZE, RECORD_SIZE)){
            writer.open();

            // write first record
            PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
            writer.write(priceUpdate);

            // write second record
            priceUpdate = new PriceUpdate(3, 4, 5);
            writer.write(priceUpdate);
        }


        // set commit flag to false for the first record
        MMapFile mem = new MMapFile(fileName, FILE_SIZE);
        mem.putByteVolatile(Structure.Data, StatusFlag.NotSet);

        try(MMapFileReader reader = new MMapFileReader(fileName, FILE_SIZE, RECORD_SIZE)) {
            reader.setTimeout(0);
            reader.open();

            assertEquals(0, reader.getTimeoutCounter());
            assertEquals(0, reader.getTimerStart());
            for (int i = 0; i < MMapFileReader.MAX_TIMEOUT_COUNT - 10; i++) {
                assertEquals(false, reader.next());
            }
            assertEquals(MMapFileReader.MAX_TIMEOUT_COUNT - 10, reader.getTimeoutCounter());
            assertEquals(0, reader.getTimerStart());

            // another reader sets the rollback flag
            mem.putByteVolatile(Structure.Data, StatusFlag.Rollback);

            // the reader skips the record
            assertEquals(false, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(0, reader.getTimeoutCounter());
            assertEquals(0, reader.getTimerStart());

            // the reader reads the second record
            PriceUpdate priceUpdate = new PriceUpdate();
            assertEquals(true, reader.next());
            assertEquals(false, reader.hasRecovered());
            assertEquals(0, reader.readType());
            reader.readMessage(priceUpdate);
            assertEquals(3, priceUpdate.getSource());
            assertEquals(4, priceUpdate.getPrice());
            assertEquals(5, priceUpdate.getQuantity());

            // no more records available
            assertEquals(false, reader.next());
            assertEquals(true, reader.hasRecovered());

            new File(fileName).delete();
        }

    }

    class PriceUpdate implements IMMapMessage {

        public static final int TYPE = 0;

        private int source;

        private int price;

        private int quantity;

        public PriceUpdate() {
        }

        public PriceUpdate(int source, int price, int quantity) {
            this.source = source;
            this.price = price;
            this.quantity = quantity;
        }

        public int type() {
            return TYPE;
        }

        public int getSource() {
            return source;
        }

        public void setSource(int source) {
            this.source = source;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "PriceUpdate [source=" + source + ", price=" + price + ", quantity=" + quantity + "]";
        }

        public void write(MMapFile mem, long pos) {
            mem.putInt(pos, source);
            mem.putInt(pos + 4, price);
            mem.putInt(pos + 8, quantity);
        }

        public void read(MMapFile mem, long pos) {
            source = mem.getInt(pos);
            price = mem.getInt(pos + 4);
            quantity = mem.getInt(pos + 8);
        }
    }
}