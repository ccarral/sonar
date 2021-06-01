package sonar.minimodem;

import java.io.*;
import org.apache.commons.codec.binary.Base32InputStream;

public class BufferedReceiver extends InputStream {

  private Base32InputStream innerReader;
  private Process rxHandle;

  public BufferedReceiver(MinimodemReceiver rx) throws IOException {
    this.rxHandle = rx.initProcess();
    InputStream stdout = this.rxHandle.getInputStream();
    this.innerReader = new Base32InputStream(stdout);
  }

  @Override
  public int read() throws IOException {
    // System.out.println("reading...");
    int ret = this.innerReader.read();
    return ret;
  }

  @Override
  public void close() {
    this.rxHandle.destroy();
  }
}
