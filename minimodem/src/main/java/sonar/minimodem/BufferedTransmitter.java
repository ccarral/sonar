package sonar.minimodem;

import java.io.*;
import org.apache.commons.codec.binary.Base32OutputStream;

public class BufferedTransmitter extends OutputStream {
  /**
   * Ésta clase se engarga de escribir bytes de manera confiable y "buffereada" por bloques a un
   * Base32OutputStream.
   */
  private Base32OutputStream innerWriter;

  private OutputStream stdin;

  private Process txHandle;

  private byte[] byteQueue;

  private MinimodemTransmitter txInstance;

  // Número de bytes que guardará en el buffer antes de hacer flush()
  private static final int BLOCK_SIZE = 50;

  private int queueSize = 0;

  public BufferedTransmitter(MinimodemTransmitter tx) throws IOException {

    this.txInstance = tx;
    this.txHandle = tx.initProcess();

    // Obtiene entrada estandar
    this.stdin = this.txHandle.getOutputStream();

    this.innerWriter = new Base32OutputStream(stdin);
    // this.byteQueue = new byte[BufferedTransmitter.BLOCK_SIZE];
  }

  public void write(int b) throws IOException {
    this.innerWriter.write(b);
  }

  public void myWrite(byte[] b, int off, int len) throws IOException, InterruptedException {
    this.innerWriter.write(b, off, len);
  }

  public Base32OutputStream getInnerWriter() {
    return this.innerWriter;
  }

  public long getEstimatedLapseOfWriting(int b) {
    // Regresa la cantidad en milisegundos que la función de write() debe de bloquear para darle
    // tiempo al proceso de escribir los caracteres a stdin
    double baudRate = 0;
    switch (this.txInstance.getBaudMode()) {
      case BELL202:
        baudRate = 1200;
        break;

      case BELL103:
        baudRate = 300;
        break;

      case RTTY:
        baudRate = 45.45;

      case TDD:
        baudRate = 45.45;
        break;

      case SAME:
        baudRate = 520.83;
        break;
    }

    // Asumiendo que en la especificación de minimodem, bps se refiere a símbolos por segundo (en
    // vez de bits)
    // y teniendo en cuenta que la base 32 codifica 4 bytes por vez, entonces
    //
    // tms = b*(1000/(BR * 4))

    return (long) (b * (1000 / baudRate));
  }

  @Override
  public void flush() throws IOException {
    this.innerWriter.flush();
  }

  @Override
  public void close() throws IOException {
    this.innerWriter.close();
    this.txHandle.destroy();
  }
}
