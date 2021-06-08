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
  public void setUp() throws InterruptedException {
    Thread.sleep(3000);
  }

  @Test
  public void testPacketCRC() {
    Packet p1 = new Packet(420, 69);

    Random random = new Random();
    int min = 0;
    int max = Packet.BUFF - Packet.HEADERS;
    int randomLen = random.ints(min, max).findFirst().getAsInt();
    for (int i = 0; i < randomLen; i++) {
      p1.write((byte) random.nextInt());
    }

    Packet p2 = new Packet(p1.data.clone());

    assertEquals(p1.getCRC32(), p2.getCRC32());
    assertEquals(p1.getAck(), p2.getAck());
    assertEquals(p1.getSeq(), p2.getSeq());
    assertEquals(p1.getDataLength(), p2.getDataLength());

    for (int i = 0; i < Packet.BUFF; i++) {
      assertEquals(p1.data[i], p2.data[i]);
    }

    // Verificar crc32
    CRC32 calculated = new CRC32();

    calculated.update(p2.getSeq());
    calculated.update(p2.getAck());

    // calculated.update(p.data, Packet.HEADERS, p.getDataLength());
    for (int i = Packet.HEADERS; i < Packet.HEADERS + p2.getDataLength(); i++) {
      calculated.update(p2.data[i]);
    }

    assertEquals(calculated.getValue(), p2.getCRC32());
  }

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

      Packet sent = new Packet(24, 10009);

      assertEquals(sent.getSeq(), 24);
      assertEquals(sent.getAck(), 10009);

      // for (int i = Packet.HEADERS; i < 60; i++) {
      // sent.write((byte) random.nextInt());
      // }
      socket.writePacket(sent);

      Packet received = socket.receivePacket();

      assertEquals(sent.getDataLength(), 0);
      assertEquals(received.getDataLength(), 0);

      assertEquals(sent.getCRC32(), received.getCRC32());

      assertEquals(sent.getSeq(), received.getSeq());
      assertEquals(sent.getAck(), received.getAck());

      // Verificar que es un paquete lleno
      // assertEquals(sent.getDataLength(), Packet.BUFF - Packet.HEADERS);

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
      System.err.println(e.toString());
      fail();
    }
  }

  @Test
  @DisplayName("Recibir un paquete en un lapso de tiempo")
  @Disabled()
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

  @Test
  @DisplayName("Probar dos veces el lockstep")
  public void testTwoLockstep() {
    try {
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Random random = new Random();

      Packet sent1 = new Packet(24, 10009);
      Packet sent2 = new Packet(48, 20009);

      for (int i = Packet.HEADERS; i < Packet.BUFF; i++) {
        sent1.write((byte) random.nextInt());
        sent2.write((byte) random.nextInt());
      }

      Packet received1 = socket.writeLockstep(sent1, SonarSocket.DELAY_MS * 2);

      Thread.sleep(SonarSocket.DELAY_MS);

      Packet received2 = socket.writeLockstep(sent2, SonarSocket.DELAY_MS * 2);

      Thread.sleep(SonarSocket.DELAY_MS);

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent1.data[i], received1.data[i]);
      }

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent2.data[i], received2.data[i]);
      }

      socket.close();

    } catch (Exception e) {
      System.err.println(e.toString());
      fail();
    }
  }
}
