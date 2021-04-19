
package sonar.minimodem;

public class MinimodemNotInPathException extends Exception{

    public String toString(){
        return "Minimodem not found in path";
    }
}
