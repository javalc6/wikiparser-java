package bzip2;
/* included in https://commons.apache.org/proper/commons-compress
   reused under Apache License version 2.0
*/

public interface InputStreamStatistics {
    /**
     * @return the amount of raw or compressed bytes read by the stream
     */
    long getCompressedCount();

    /**
     * @return the amount of decompressed bytes returned by the stream
     */
    long getUncompressedCount();
}
