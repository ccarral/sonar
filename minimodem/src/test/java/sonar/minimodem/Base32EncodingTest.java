package sonar.minimodem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Random;
import org.apache.commons.codec.binary.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

public class Base32EncodingTest {
  InputStream input;
  byte[] testString;

  @BeforeEach
  public void setUp() throws InterruptedException {

    Thread.sleep(3000);
    int min = 700;
    int max = 1000;
    Random random = new Random();
    int randomLen = random.ints(min, max).findFirst().getAsInt();
    // randomLen = 400;

    this.testString = new byte[randomLen];

    random.nextBytes(testString);

    this.input = new ByteArrayInputStream(testString);
  }

  @DisplayName("Probar que la codificación base 32 funciona como esperamos.")
  @Disabled("Tardado")
  @Test
  public void testBase32BothWays() {
    try {

      ProcessBuilder pb = new ProcessBuilder("cat");

      Process catProcess = pb.start();

      // STDIN del proceso
      OutputStream outputStream = catProcess.getOutputStream();
      Base32OutputStream base32OutputStream = new Base32OutputStream(outputStream);

      // STDOUT del proceso
      InputStream inputStream = catProcess.getInputStream();
      Base32InputStream base32InputStream = new Base32InputStream(inputStream);

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(base32OutputStream));
      BufferedReader reader = new BufferedReader(new InputStreamReader(base32InputStream));

      String testString = "Hola mamá, ¿cómo estás?";

      writer.write(testString);
      writer.flush();
      writer.close();

      String read = reader.readLine();

      assertEquals(read, testString);

    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @DisplayName("Probar que Minimodem puede transmitir bytes arbitrarios codificados en base 32")
  @EnabledIfEnvironmentVariable(named = "FULL", matches = "1")
  @Test
  public void testBase32MinimodemTxRxArbitraryBytes() {
    try {
      System.out.println(
          "Nota: puede tomar un poco de tiempo dependiendo del número de bytes elegido al azar.");

      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);

      Process txProcess = tx.initProcess();
      Process rxProcess = rx.initProcess();

      OutputStream txOutputStream = txProcess.getOutputStream();
      InputStream rxInputStream = rxProcess.getInputStream();

      Base32OutputStream base32OutputStream = new Base32OutputStream(txOutputStream);
      Base32InputStream base32InputStream = new Base32InputStream(rxInputStream);

      byte[] buffer = new byte[1024];
      int read = 0;
      while ((read = this.input.read(buffer)) != -1) {
        base32OutputStream.write(buffer, 0, read);
      }
      base32OutputStream.flush();
      base32OutputStream.close();

      int i = 0;
      int ch;

      byte[] outBuf = new byte[this.testString.length];

      while ((ch = base32InputStream.read()) != -1) {
        outBuf[i++] = (byte) ch;
        if (i == this.testString.length) {
          base32InputStream.close();
          break;
        }
      }

      rxInputStream.close();

      txProcess.destroy();
      rxProcess.destroy();

      for (int j = 0; j < this.testString.length; j++) {
        assertEquals(outBuf[j], this.testString[j]);
      }

    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
