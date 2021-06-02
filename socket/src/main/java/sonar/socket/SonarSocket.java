package sonar.socket;

import java.io.*;
import org.apache.commons.codec.binary.Base32OutputStream;
import sonar.minimodem.*;

public class SonarSocket {
  // private ObjectOutputStream objectOutputStream;
  // private ObjectInputStream objectInputStream;

  private BufferedTransmitter innerOutputStream;
  private BufferedReceiver innerInputStream;

  public SonarSocket(BufferedReceiver in, BufferedTransmitter out) throws IOException {
    this.innerInputStream = in;
    this.innerOutputStream = out;

    // this.objectInputStream = new ObjectInputStream(in);
    // this.objectOutputStream = new ObjectOutputStream(out);
  }

  public void writePacket(Packet packet) throws IOException {
    for (int i = 0; i < Packet.BUFF; i++) {
      this.innerOutputStream.write(packet.data[i]);
      this.innerOutputStream.flush();
    }

    this.innerOutputStream.getInnerWriter().close();
  }

  public Packet receivePacket() throws IOException {
    int byteCount = 0;
    Packet p = new Packet(0, 1);
    int b;
    while ((b = this.innerInputStream.read()) != -1) {
      // System.out.println(byteCount);
      p.data[byteCount++] = (byte) b;
      if (byteCount == Packet.BUFF) {
        break;
      }
    }

    return p;
  }

  public Base32OutputStream getBase32OutputStream() {
    return this.innerOutputStream.getInnerWriter();
  }

  public void close() throws IOException {
    this.innerOutputStream.close();
    this.innerInputStream.close();
  }
}
