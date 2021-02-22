package io;

import java.io.EOFException;
import java.io.IOException;

/**
 * Memory Map File Writer
 */
public class MMapFileWriter implements AutoCloseable{
    private MMapFile mem;
    private final String fileName;
    private final long fileSize;
    private final int entrySize;

    public MMapFileWriter(String fileName, long fileSize, int recordSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.entrySize = recordSize + Constants.Length.RecordHeader;
    }

    /**
     * Open the MMapFile
     * @throws IOException
     */
    public void open() throws IOException {
        try {
            mem = new MMapFile(fileName, fileSize);
        } catch(Exception e) {
            throw new IOException("Unable to open the file: " + fileName, e);
        }
        mem.compareAndSwapLong(Constants.Structure.Limit, 0, Constants.Structure.Data);
    }

    /**
     * Write the input message to the file.
     * @param message
     * @return
     * @throws EOFException
     */
    public boolean write(IMMapMessage message) throws EOFException {
        long commitPos = writeRecord(message);
        return commit(commitPos);
    }

    /**
     * Write the input message to the file.
     * @param message
     * @return
     * @throws EOFException
     */
    protected long writeRecord(IMMapMessage message) throws EOFException {
        long limit = allocate();
        long commitPos = limit;
        limit += Constants.Length.StatusFlag;
        mem.putInt(limit, message.type());
        limit += Constants.Length.Metadata;
        message.write(mem, limit);
        return commitPos;
    }

    /**
     * Write the input bytes from the offset and upto the length
     * mentioned.
     * @param src
     * @param offset
     * @param length
     * @return
     * @throws EOFException
     */
    public boolean write(byte[] src, int offset, int length) throws EOFException {
        long commitPos = writeRecord(src, offset, length);
        return commit(commitPos);
    }

    /**
     * Write input byte source from the offset and upto the length mentioned.
     * @param src
     * @param offset
     * @param length
     * @return
     * @throws EOFException
     */
    protected long writeRecord(byte[] src, int offset, int length) throws EOFException {
        long limit = allocate();
        long commitPos = limit;
        limit += Constants.Length.StatusFlag;
        mem.putInt(limit, length);
        limit += Constants.Length.Metadata;
        mem.setBytes(limit, src, offset, length);
        return commitPos;
    }

    /**
     * Allocate memory.
     * @return
     * @throws EOFException
     */
    private long allocate() throws EOFException {
        long limit = mem.getAndAddLong(Constants.Structure.Limit, entrySize);
        if (limit + entrySize > fileSize) {
            throw new EOFException("End of file was reached");
        }
        return limit;
    }

    /**
     * Commit the position to which data has been written.
     * @param commitPos
     * @return
     */
    protected boolean commit(long commitPos) {
        return mem.compareAndSwapInt(commitPos, Constants.StatusFlag.NotSet, Constants.StatusFlag.Commit);
    }

    /**
     * Close the writer.
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            mem.unmap();
        } catch(Exception e) {
            throw new IOException("Unable to close the file", e);
        }
    }
}
