package sonar.listen;

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

      // Inmediatamente después comienza la transmisión del lado del tx

      socket.receiveAllPackets();

      for (byte b : socket.incomingByteList) {
        System.out.printf("%02X", b);
      }

    } catch (Exception e) {
      System.err.println("Error escuchando");
      System.err.println(e.toString());
    }
  }
}
