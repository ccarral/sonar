package sonar.minimodem;

import java.io.IOException;

public class MinimodemTransmitter extends MinimodemInstance {

  public MinimodemTransmitter(BaudMode baudMode)
      throws IOException, InterruptedException, MinimodemNotInPathException {
    super(baudMode);
  }

  /* Regresa un proceso que ejecuta minimodem
   * */
  public Process initProcess() throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder("minimodem", "--tx", this.getBaudModeStr());
    Process process = processBuilder.start();
    return process;
  }
}
