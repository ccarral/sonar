package sonar.minimodem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MinimodemTest {
  InputStream input;
  String testString;

  @BeforeEach
  public void setUp() {

    this.testString = "HOLA ESTO ES TEXTO ANSI";
    this.input = new ByteArrayInputStream(testString.getBytes());
  }

  @DisplayName("Instanciadores.")
  @Test
  public void testConstructors() {

    try {
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.RTTY);
      Assertions.assertEquals(tx.getBaudMode(), BaudMode.RTTY);
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  @Disabled("Genera ruido (literalmente)")
  @DisplayName("Escribir al OutputStream del proceso.")
  @Test
  public void testInput() {
    try {
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.RTTY);
      Process process = tx.initProcess();
      OutputStream outputStream = process.getOutputStream();
      byte[] buffer = new byte[1024];
      int read = 0;
      while ((read = this.input.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
        outputStream.flush();
      }

    } catch (Exception e) {
      System.out.println(e);
    }
  }

  @DisplayName("Enviar y recibir en dos procesos en la misma máquina")
  // @Disabled("Genera ruido (literalmente)")
  @Test
  public void testSendRecv() {
    try {
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      Process txProcess = tx.initProcess();
      Process rxProcess = rx.initProcess();

      // STDIN del proceso de tx
      OutputStream outputStream = txProcess.getOutputStream();

      // STDOUT del proceso de rx
      InputStream inputStream = rxProcess.getInputStream();
      byte[] buffer = new byte[1024];
      int read = 0;
      while ((read = this.input.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }

      outputStream.flush();
      outputStream.close();

      int ch;
      StringBuilder sb = new StringBuilder();

      int i = 0;
      while ((ch = inputStream.read()) != -1) {

        sb.append((char) ch);
        i++;
        if (i == 23) {
          inputStream.close();
          break;
        }
      }

      String received = sb.toString();

      assertEquals(this.testString, received);

    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
