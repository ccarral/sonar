package sonar.socket;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Packet implements Serializable {
  public byte[] data;

  private static final int ACK = 0;
  private static final int SEQ = 4;
  private static final int DATA_LEN = 8;
  private static final int CRC32 = 16;
  public static final int BUFF = 128;
  // Bytes que ocupan ack, seq, crc32 y dataLen
  public static final int HEADERS = 24;

  private int cursor;
  private CRC32 crc32;

  public Packet(int seq, int ack) {
    this.data = new byte[BUFF];

    this.crc32 = new CRC32();

    ByteBuffer ackBuf = ByteBuffer.allocate(4);
    ackBuf.putInt(ack);
    byte[] ackArray = ackBuf.array();

    this.data[ACK] = ackArray[0];
    this.data[ACK + 1] = ackArray[1];
    this.data[ACK + 2] = ackArray[2];
    this.data[ACK + 3] = ackArray[3];

    ByteBuffer seqBuf = ByteBuffer.allocate(4);
    seqBuf.putInt(seq);
    byte[] seqArray = seqBuf.array();

    this.data[SEQ] = seqArray[0];
    this.data[SEQ + 1] = seqArray[1];
    this.data[SEQ + 2] = seqArray[2];
    this.data[SEQ + 3] = seqArray[3];

    this.setDataLength(0);

    // Posiciona el cursor justo después de los headers
    this.cursor = HEADERS;
  }

  public int getAck() {
    byte[] ack = new byte[4];
    for (int i = 0; i < 4; i++) {
      ack[i] = this.data[ACK + i];
    }
    return ByteBuffer.wrap(ack).getInt();
  }

  public long getSeq() {
    byte[] seq = new byte[4];
    for (int i = 0; i < 4; i++) {
      seq[i] = this.data[SEQ + i];
    }
    return ByteBuffer.wrap(seq).getInt();
  }

  public void write(byte b) throws IndexOutOfBoundsException {
    if (this.cursor == BUFF) {
      throw new IndexOutOfBoundsException();
    } else {
      data[cursor++] = b;
      this.incrementDataLength();
      this.updateCRC(b);
    }
  }

  public int getDataLength() {
    byte[] dataLen = new byte[4];
    for (int i = 0; i < 4; i++) {
      dataLen[i] = this.data[DATA_LEN + i];
    }
    return ByteBuffer.wrap(dataLen).getInt();
  }

  private void setDataLength(int i) {
    ByteBuffer buf = ByteBuffer.allocate(4);
    buf.putInt(i);
    byte[] array = buf.array();
    this.data[DATA_LEN] = array[0];
    this.data[DATA_LEN + 1] = array[1];
    this.data[DATA_LEN + 2] = array[2];
    this.data[DATA_LEN + 3] = array[3];
  }

  private void incrementDataLength() {
    int previousDataLen = this.getDataLength();
    this.setDataLength(++previousDataLen);
  }

  private void setCRC32(long v) {

    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.putLong(v);
    byte[] array = buf.array();
    this.data[CRC32] = array[0];
    this.data[CRC32 + 1] = array[1];
    this.data[CRC32 + 2] = array[2];
    this.data[CRC32 + 3] = array[3];
    this.data[CRC32 + 4] = array[4];
    this.data[CRC32 + 5] = array[5];
    this.data[CRC32 + 6] = array[6];
    this.data[CRC32 + 7] = array[7];
  }

  public long getCRC32() {
    byte[] crc32 = new byte[8];
    for (int i = 0; i < 8; i++) {
      crc32[i] = this.data[CRC32 + i];
    }
    return ByteBuffer.wrap(crc32).getLong();
  }

  private void updateCRC(int b) {
    this.crc32.update(b);
    long currentCRC32 = this.crc32.getValue();
    this.setCRC32(currentCRC32);
  }
}
