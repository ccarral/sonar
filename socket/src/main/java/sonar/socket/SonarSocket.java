package sonar.socket;

import java.io.*;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base32OutputStream;
import sonar.minimodem.*;

public class SonarSocket {

  private BufferedTransmitter innerOutputStream;
  private BufferedReceiver innerInputStream;

  public SonarSocket(BufferedReceiver in, BufferedTransmitter out) throws IOException {
    this.innerInputStream = in;
    this.innerOutputStream = out;
  }

  public void writePacket(Packet packet) throws IOException {
    for (int i = 0; i < Packet.BUFF; i++) {
      this.innerOutputStream.write(packet.data[i]);
      this.innerOutputStream.flush();
    }

    this.innerOutputStream.getInnerWriter().close();
  }

  public Packet receivePacket() throws IOException {

    // Detectar número mágico
    byte[] magic = new byte[4];

    while (true) {
      this.innerInputStream.read(magic);
      if (Arrays.equals(Packet.MAGIC_BYTES, magic)) {
        break;
      }
    }

    int byteCount = Packet.MAGIC_BYTES.length;

    Packet p = new Packet(0, 1);
    int b;
    while ((b = this.innerInputStream.read()) != -1) {
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
