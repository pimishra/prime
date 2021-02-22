package messages;

import io.MMapFile;
import io.IMMapMessage;

public class MMapInteger implements IMMapMessage {
    public static final int TYPE = 0;
    private int data;

    public MMapInteger() {
    }

    public MMapInteger(int data) {
        this.data = data;
    }

    public int type() {
        return TYPE;
    }

    public int getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MemoryMappedInteger [data=" + data + "]";
    }

    @Override
    public void write(MMapFile mem, long pos) {
        mem.putInt(pos, data);
    }

    @Override
    public void read(MMapFile mem, long pos) {
        data = mem.getInt(pos);
    }
}
