package messages;

import io.MMapFile;
import io.IMMapMessage;

public class MMapPrimeResponse implements IMMapMessage {
    public static final int TYPE = 1;
    private int data;
    private boolean isPrime;

    public MMapPrimeResponse() {
    }

    public MMapPrimeResponse(int data, boolean isPrime) {
        this.data = data;
        this.isPrime = isPrime;
    }

    public int type() {
        return TYPE;
    }

    public int getData() {
        return data;
    }

    public boolean isPrime() {
        return isPrime;
    }

    @Override
    public String toString() {
        return "MMapPrimeResponse [data=" + data + ", isPrime=" + isPrime + "]";
    }

    @Override
    public void write(MMapFile mem, long pos) {
        mem.putInt(pos, data);
        mem.putInt(pos+4, isPrime ? 1 : 0);
    }

    @Override
    public void read(MMapFile mem, long pos) {
        data = mem.getInt(pos);
        isPrime = mem.getInt(pos+4) == 1 ? true : false;
    }
}
