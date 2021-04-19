
package sonar.minimodem;

class InvalidBaudModeException extends Exception{
    private int invalidBaudMode;
    InvalidBaudModeException(int baudMode){
        this.invalidBaudMode = baudMode;
    }

    public String toString(){
        return "Invalid baud mode: " + this.invalidBaudMode;
    }
}
