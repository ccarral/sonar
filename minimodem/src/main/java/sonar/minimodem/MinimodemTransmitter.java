package sonar.minimodem;

import java.io.IOException;

public class MinimodemTransmitter{
    private String baudModeStr;
    private BaudMode baudMode;

    public MinimodemTransmitter(BaudMode baudMode) throws IOException,InterruptedException, MinimodemNotInPathException{

        // Check that minimodem is in path
        Process minimodem = Runtime.getRuntime().exec("minimodem");
        int retcode = minimodem.waitFor();

        if(retcode != 1){
            throw new MinimodemNotInPathException();
        }

        this.baudMode = baudMode;

        switch(baudMode){
            case BELL202:
                this.baudModeStr = "1200";
                break;

            case BELL103:
                this.baudModeStr ="300";
                break;
            case RTTY:
                this.baudModeStr = "rtty";
                break;

            case TDD:
                this.baudModeStr = "same";
                break;

            case SAME:
                this.baudModeStr = "same";
                break;

        }


    }

    public BaudMode getBaudMode(){
        return this.baudMode;
    }


    public Process initProcess() throws IOException{
        ProcessBuilder processBuilder = new ProcessBuilder("minimodem", "--tx", String.valueOf(this.baudModeStr));
        Process process = processBuilder.start();
        return process;
    }

}
