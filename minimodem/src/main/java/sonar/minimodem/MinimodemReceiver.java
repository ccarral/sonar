package sonar.minimodem;

import java.io.IOException;

public class MinimodemReceiver extends MinimodemInstance {
  public static final double CONFIDENCE = 3.0;

  public MinimodemReceiver(BaudMode baudMode)
      throws IOException, InterruptedException, MinimodemNotInPathException {
    super(baudMode);
  }

  public Process initProcess() throws IOException {
    ProcessBuilder processBuilder =
        new ProcessBuilder(
            "minimodem",
            "--rx",
            "--confidence",
            String.valueOf(MinimodemReceiver.CONFIDENCE),
            this.getBaudModeStr());
    Process process = processBuilder.start();
    return process;
  }
}
