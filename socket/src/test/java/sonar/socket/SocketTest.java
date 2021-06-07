package sonar.socket;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import sonar.minimodem.*;

public class SocketTest {
  @BeforeEach
  public void setUp() {}

  @Test
  @DisplayName("Enviar y recibir un paquete")
  // @Disabled()
  public void sendSinglePacket() {

    try {
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Random random = new Random();

      Packet sent = new Packet(24, 10009);

      assertEquals(sent.getSeq(), 24);
      assertEquals(sent.getAck(), 10009);

      for (int i = Packet.HEADERS; i < Packet.BUFF; i++) {
        sent.write((byte) random.nextInt());
      }

      socket.writePacket(sent);

      Packet received = socket.receivePacket();

      assertEquals(sent.getSeq(), received.getSeq());
      assertEquals(sent.getAck(), received.getAck());

      // Verificar que es un paquete lleno
      assertEquals(sent.getDataLength(), Packet.BUFF - Packet.HEADERS);

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent.data[i], received.data[i]);
      }

      // Verificar que coinciden los crc
      CRC32 crc32 = new CRC32();
      crc32.update(received.getSeq());
      crc32.update(received.getAck());
      crc32.update(received.data, Packet.HEADERS, received.getDataLength());

      long calculated = crc32.getValue();
      long got = received.getCRC32();

      assertEquals(got, calculated);

      socket.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Recibir un paquete en un lapso de tiempo")
  public void testReceiveTimedPacket() {
    try {
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Random random = new Random();

      Packet sent = new Packet(24, 10009);

      for (int i = Packet.HEADERS; i < Packet.BUFF; i++) {
        sent.write((byte) random.nextInt());
      }

      socket.writePacket(sent);

      Future<Packet> future = socket.timedReceivePacket0();

      Packet received = future.get(SonarSocket.DELAY_MS, TimeUnit.MILLISECONDS);

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent.data[i], received.data[i]);
      }

      socket.close();

    } catch (Exception e) {
      fail("El paquete no se mandó en el timeout");
    }
  }

  @Test
  @DisplayName("Test de writeLockStep con bandera EOF")
  public void testWriteLockstep() {
    try {
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Random random = new Random();

      Packet sent = new Packet(24, 10009);

      for (int i = Packet.HEADERS; i < Packet.BUFF; i++) {
        sent.write((byte) random.nextInt());
      }

      socket.signalEOF();

      Packet received = socket.writeLockstep(sent, SonarSocket.DELAY_MS * 2);

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent.data[i], received.data[i]);
      }

      assertTrue(received.getEOF());

      socket.close();

    } catch (Exception e) {
      fail("El paquete no se mandó en el timeout");
    }
  }
}
