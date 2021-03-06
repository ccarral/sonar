package sonar.minimodem;

import java.io.IOException;

public abstract class MinimodemInstance {
  private String baudModeStr;
  private BaudMode baudMode;

  public MinimodemInstance(BaudMode baudMode)
      throws IOException, InterruptedException, MinimodemNotInPathException {

    // Verificar que minimodem está en el path.
    Process minimodem = Runtime.getRuntime().exec("minimodem");
    int retcode = minimodem.waitFor();

    if (retcode != 1) {
      throw new MinimodemNotInPathException();
    }

    this.baudMode = baudMode;

    switch (baudMode) {
      case BELL202:
        this.baudModeStr = "1200";
        break;

      case BELL103:
        this.baudModeStr = "300";
        break;
      case RTTY:
        this.baudModeStr = "rtty";
        break;

      case TDD:
        this.baudModeStr = "tdd";
        break;

      case SAME:
        this.baudModeStr = "same";
        break;
    }
  }

  public BaudMode getBaudMode() {
    return this.baudMode;
  }

  public String getBaudModeStr() {
    return this.baudModeStr;
  }
}
