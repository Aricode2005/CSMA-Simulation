package csma;

import java.util.zip.CRC32;

public class Frame {
    private String sourceAddress;      // 6 bytes (simulated as String)
    private String destinationAddress; // 6 bytes (simulated as String)
    private int length;                // 2 bytes
    private int seqNo;                 // 1 byte
    private byte[] payload;            // 46-1500 bytes
    private long fcs;                  // 4 bytes (Frame Check Sequence / CRC)

    public Frame(String sourceAddress, String destinationAddress, int seqNo, byte[] payload) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.seqNo = seqNo;
        this.payload = payload;
        this.length = payload.length;
        this.fcs = calculateCRC(payload);
    }

    private long calculateCRC(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    public String getSourceAddress() { return sourceAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public int getLength() { return length; }
    public int getSeqNo() { return seqNo; }
    public byte[] getPayload() { return payload; }
    public long getFcs() { return fcs; }

    @Override
    public String toString() {
        return String.format("[Frame %d | Src: %s | Dst: %s | Len: %d | CRC: %d | Data: %s]",
                seqNo, sourceAddress, destinationAddress, length, fcs, new String(payload).trim());
    }
}
