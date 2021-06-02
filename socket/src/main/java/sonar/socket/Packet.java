package sonar.socket;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class Packet implements Serializable {
  public byte[] data;

  private static final int ACK = 0;
  private static final int SEQ = 4;
  public static final int BUFF = 64;

  // Bytes que ocupan ack y seq
  public static final int HEADERS = 8;
  private int cursor;

  public Packet(int seq, int ack) {
    this.data = new byte[BUFF];

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
    }
  }
}
