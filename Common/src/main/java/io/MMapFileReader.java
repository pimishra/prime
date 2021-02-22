package io;

import java.io.EOFException;
import java.io.IOException;

/**
 * Memory Map File Reader
 */
public class MMapFileReader implements AutoCloseable{
    public static final long MAX_TIMEOUT_COUNT = 100;
    private final String fileName;
    private final long fileSize;
    private final int recordSize;
    private MMapFile mem;
    private long limit = Constants.Structure.Data;
    private long prevLimit = 0;
    private long initialLimit;
    private int maxTimeout = 2000;
    protected long timerStart;
    protected long timeoutCounter;
    private boolean typeRead;

    public MMapFileReader(String fileName, long fileSize, int recordSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.recordSize = recordSize;
    }

    /**
     * Open the reader
     * @throws IOException
     */
    public void open() throws IOException {
        try {
            mem = new MMapFile(fileName, fileSize);
        } catch(Exception e) {
            throw new IOException("Unable to open the file: " + fileName, e);
        }
        initialLimit = mem.getLongVolatile(Constants.Structure.Limit);
    }

    /**
     * Set time out for the read operation.
     * @param timeout
     */
    public void setTimeout(int timeout) {
        this.maxTimeout = timeout;
    }

    /**
     * Set all pointers to the next available read position.
     * @return
     * @throws EOFException
     */
    public boolean next() throws EOFException {
        if (limit >= fileSize) {
            throw new EOFException("End of file was reached");
        }
        if (prevLimit != 0 && limit - prevLimit < Constants.Length.RecordHeader + recordSize) {
            limit = prevLimit + Constants.Length.RecordHeader + recordSize;
        }
        if (mem.getLongVolatile(Constants.Structure.Limit) <= limit) {
            return false;
        }
        int statusFlag = mem.getIntVolatile(limit);
        if (statusFlag == Constants.StatusFlag.Rollback) {
            limit += Constants.Length.RecordHeader + recordSize;
            prevLimit = 0;
            timeoutCounter = 0;
            timerStart = 0;
            return false;
        }
        if (statusFlag == Constants.StatusFlag.Commit) {
            timeoutCounter = 0;
            timerStart = 0;
            prevLimit = limit;
            return true;
        }
        timeoutCounter++;
        if (timeoutCounter >= MAX_TIMEOUT_COUNT) {
            if (timerStart == 0) {
                timerStart = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - timerStart >= maxTimeout) {
                    if (!mem.compareAndSwapInt(limit, Constants.StatusFlag.NotSet, Constants.StatusFlag.Rollback)) {
                        return false;
                    }
                    limit += Constants.Length.RecordHeader + recordSize;
                    prevLimit = 0;
                    timeoutCounter = 0;
                    timerStart = 0;
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Read type of the message
     * @return
     */
    public int readType() {
        typeRead = true;
        limit += Constants.Length.StatusFlag;
        int type = mem.getInt(limit);
        limit += Constants.Length.Metadata;
        return type;
    }

    /**
     * Read the next available message and populate to the input argument.
     * @param message
     * @return
     */
    public IMMapMessage readMessage(IMMapMessage message) {
        if (!typeRead) {
            readType();
        }
        typeRead = false;
        message.read(mem, limit);
        limit += recordSize;
        return message;
    }

    /**
     * Read the byte buffer from the given offset.
     * @param dst
     * @param offset
     * @return
     */
    public int readBuffer(byte[] dst, int offset) {
        limit += Constants.Length.StatusFlag;
        int length = mem.getInt(limit);
        limit += Constants.Length.Metadata;
        mem.getBytes(limit, dst, offset, length);
        limit += recordSize;
        return length;
    }

    public boolean hasRecovered() {
        return limit >= initialLimit;
    }

    /**
     * Close the reader
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            mem.unmap();
        } catch(Exception e) {
            throw new IOException("Unable to close the file", e);
        }
    }

    public long getTimeoutCounter(){
        return timeoutCounter;
    }

    public long getTimerStart(){
        return timerStart;
    }
}
