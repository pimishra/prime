package io;

public class Constants {
    public final static String MMAP_DIR = "C:/Temp";
    public final static String REQUESTS_MMAP = "C:/Temp/PrimeRequests.dat";
    public final static String RESPONSE_MMAP = "C:/Temp/PrimeResponses.dat";
    public final static long FILE_SIZE = 100000000L;
    public final static int RECORD_SIZE = 20;

    public static class Structure {
        public static final int Limit = 0;

        public static final int Data = Length.Limit;
    }

    public static class Length {
        public static final int Limit = 8;

        public static final int StatusFlag = 4;

        public static final int Metadata = 4;

        public static final int RecordHeader = StatusFlag + Metadata;
    }

    public static class StatusFlag {
        public static final byte NotSet = 0;

        public static final byte Commit = 1;

        public static final byte Rollback = 2;
    }
}
