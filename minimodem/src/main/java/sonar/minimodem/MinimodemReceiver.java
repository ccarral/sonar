package sonar.minimodem;

import java.io.IOException;

public class MinimodemReceiver extends MinimodemInstance {

  public MinimodemReceiver(BaudMode baudMode)
      throws IOException, InterruptedException, MinimodemNotInPathException {
    super(baudMode);
  }

  public Process initProcess() throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder("minimodem", "--rx", this.getBaudModeStr());
    Process process = processBuilder.start();
    return process;
  }
}
