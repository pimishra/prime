package io;

/**
 * MMap message interface. All classes written to MMapFile
 * must implement this interface.
 */
public interface IMMapMessage {
    /**
     * Write to MMap file at input position
     * @param mem
     * @param pos
     */
    void write(MMapFile mem, long pos);

    /**
     * Read from MMapFile at input position.
     * @param mem
     * @param pos
     */
    void read(MMapFile mem, long pos);

    /**
     * Return type of message. This uniquely defines the message.
     * @return
     */
    int type();
}
