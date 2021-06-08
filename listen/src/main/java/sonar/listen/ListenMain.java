package sonar.listen;

import java.io.*;
import sonar.minimodem.*;
import sonar.socket.*;

public class ListenMain {
  public static void main(String[] args) {
    try {
      System.out.println("shh! listen...");
      MinimodemReceiver rx = new MinimodemReceiver(BaudMode.BELL202);
      MinimodemTransmitter tx = new MinimodemTransmitter(BaudMode.BELL202);

      BufferedTransmitter transmitter = new BufferedTransmitter(tx);
      BufferedReceiver receiver = new BufferedReceiver(rx);

      SonarSocket socket = new SonarSocket(receiver, transmitter);

      Packet sync = socket.receivePacket();
      System.out.println("Recibiendo paquete de sincronización");

      socket.writePacket(sync);
      System.out.println("Paquete de sincronización mandado");

      byte[] bytesNombre = new byte[16];

      for (int i = 0; i < 16; i++) {
        bytesNombre[i] = sync.data[Packet.HEADERS + i];
      }

      String nombre = new String(bytesNombre);

      String trimmed = nombre.trim();

      System.out.println("Nombre recibido:" + nombre);

      File archivo = new File(trimmed);

      archivo.createNewFile();

      FileOutputStream fos = new FileOutputStream(archivo);

      for (int i = 16; i < sync.getDataLength(); i++) {
        fos.write(sync.data[Packet.HEADERS + i]);
      }

      fos.close();

    } catch (Exception e) {
      System.err.println("Error escuchando");
      System.err.println(e.toString());
    }
  }
}
