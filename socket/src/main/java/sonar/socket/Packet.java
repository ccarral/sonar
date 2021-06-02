package sonar.socket;

import java.io.Serializable;

public class Packet implements Serializable {
  public byte[] data;

  private static final int ACK = 1;
  private static final int SEQ = 1;
  public static final int BUFF = 64;

  // Bytes que ocupan ack y seq
  public static final int HEADERS = 2;
  private int ack;
  private int seq;
  private int cursor;

  public Packet(int seq, int ack) {
    this.data = new byte[BUFF];
    this.data[SEQ] = 0;
    this.data[ACK] = 1;
    this.seq = seq;
    this.ack = ack;
    this.cursor = HEADERS;
  }

  public int getAck() {
    return 1;
  }

  public long getSeq() {
    return 0;
  }

  public void write(byte b) throws IndexOutOfBoundsException {
    if (this.cursor == BUFF) {
      throw new IndexOutOfBoundsException();
    } else {
      data[cursor++] = b;
    }
  }
}
