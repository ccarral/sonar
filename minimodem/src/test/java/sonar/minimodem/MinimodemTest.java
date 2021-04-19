
package sonar.minimodem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class MinimodemTest{
    InputStream input; 
    String testString;

    @BeforeEach
    public void setUp(){

        this.testString = "Muchos años después, frente al pelotón de fusilamiento, el coronel Aureliano Buendía había de recordar aquella tarde remota en que su padre lo llevó a conocer el hielo.";
        this.input = new ByteArrayInputStream(testString.getBytes());
    }

    @DisplayName("Instanciadores.")
    @Test public void testConstructors(){
        
        try{
            MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.RTTY);
            Assertions.assertEquals(tx.getBaudMode(),BaudMode.RTTY);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Disabled("Genera ruido (literalmente)")
    @DisplayName("Escribir al OutputStream del proceso.")
    @Test public void testInput(){
        try{
            MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.RTTY);
            Process process = tx.initProcess();
            OutputStream outputStream = process.getOutputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while((read = this.input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
                
        }catch(Exception e){
            System.out.println(e);
        }
            
    }



}
