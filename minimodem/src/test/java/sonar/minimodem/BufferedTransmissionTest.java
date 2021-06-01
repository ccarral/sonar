package sonar.minimodem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Random;
import org.junit.jupiter.api.*;

public class BufferedTransmissionTest {

  byte[] testBuf;
  InputStream input;

  @BeforeEach
  public void setUp() throws InterruptedException {
    Thread.sleep(3000);
    Random random = new Random();
    int min = 700;
    int max = 1000;
    int randomLen = random.ints(min, max).findFirst().getAsInt();
    // int randomLen = 400;
    this.testBuf = new byte[randomLen];
    random.nextBytes(testBuf);
    this.input = new ByteArrayInputStream(this.testBuf);
  }

  @DisplayName("Probar tx y rx buffereada.")
  // @Disabled("Tardado")
  @Test
  public void testTxRx() {
    try {

      System.out.println(
          "Nota: puede tomar un poco de tiempo dependiendo del número de bytes elegido al azar.");

      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      byte[] buffer = new byte[1024];
      int read = 0;
      while ((read = this.input.read(buffer)) != -1) {
        transmitter.myWrite(buffer, 0, read);
      }

      transmitter.flush();
      transmitter.getInnerWriter().close();

      int i = 0;
      int ch;
      byte[] outBuf = new byte[this.testBuf.length];

      while ((ch = receiver.read()) != -1) {
        outBuf[i++] = (byte) ch;
        if (i == this.testBuf.length) {
          receiver.close();
          break;
        }
      }

      for (int j = 0; j < this.testBuf.length; j++) {
        assertEquals(outBuf[j], this.testBuf[j]);
      }

    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
