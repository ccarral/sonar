package sonar.socket;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
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
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Random random = new Random();

      Packet sent = new Packet(0, 1);

      for (int i = Packet.HEADERS; i < Packet.BUFF; i++) {
        sent.write((byte) random.nextInt());
      }

      socket.writePacket(sent);

      Packet received = socket.receivePacket();

      assertEquals(sent.getSeq(), received.getSeq());
      assertEquals(sent.getAck(), received.getAck());

      for (int i = 0; i < Packet.BUFF; i++) {
        assertEquals(sent.data[i], received.data[i]);
      }

      socket.close();

    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
