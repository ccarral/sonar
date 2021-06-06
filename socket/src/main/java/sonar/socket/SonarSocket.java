package sonar.socket;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import org.apache.commons.codec.binary.Base32OutputStream;
import sonar.minimodem.*;

public class SonarSocket {

  private BufferedTransmitter innerOutputStream;
  private BufferedReceiver innerInputStream;

  private static final int NO_SEQ = 0;
  private static final int NO_ACK = 1;

  public static final long DELAY_MS = 2300;

  private boolean globalExceptionFlag;

  private Throwable lastException;

  private static final int RETRIES = 3;

  private int seqCount;

  // Activado cuando un ACK para un paquete no fue recibido despues de RETRIES
  private boolean timeout;

  // Lista en donde se guardan los Ack que vienen de regreso
  private LinkedList<Integer> incomingAckList;

  // Lista de donde se obtienen los números de ack siguientes
  private LinkedList<Integer> outGoingAckList;

  // Lista de Packets que no han recibido ack de vuelta
  private LinkedList<Packet> packetsToBeAckd;

  // Número de intentos por ack
  // Si uno de estos tiene más de 3, regresa excepción de timout
  private Map<Integer, Integer> tries;

  private Packet currentPacket;

  private ScheduledExecutorService defaultScheduledExecutor;

  public SonarSocket(BufferedReceiver in, BufferedTransmitter out) throws IOException {
    this.innerInputStream = in;
    this.innerOutputStream = out;

    this.seqCount = NO_SEQ;

    this.timeout = false;

    this.globalExceptionFlag = false;

    this.outGoingAckList = new LinkedList<Integer>();

    this.incomingAckList = new LinkedList<Integer>();

    this.packetsToBeAckd = new LinkedList<Packet>();

    this.tries = Collections.synchronizedMap(new HashMap<Integer, Integer>());

    this.defaultScheduledExecutor = new ScheduledThreadPoolExecutor(2);

    this.currentPacket = new Packet(this.getNextSeq(), this.getNextAck());
  }

  private void setGlobalTimeoutFlag(boolean value) {
    this.timeout = value;
  }

  public void writePacket(Packet packet) throws IOException {
    for (int i = 0; i < Packet.BUFF; i++) {
      this.innerOutputStream.write(packet.data[i]);
      this.innerOutputStream.flush();
    }

    this.innerOutputStream.getInnerWriter().close();
  }

  // Envía un paquete y si no se recibe un ack en cierto tiempo,
  // se activa una bandera para reenvío
  public void writeTimedPacket(
      Packet packet,
      long timeout,
      HashMap<Integer, Integer> tries,
      LinkedList<Packet> packetsToBeAcked)
      throws TimeoutException {}

  public Packet receivePacket() throws IOException {

    // Detectar número mágico
    byte[] magic = new byte[4];

    while (true) {
      this.innerInputStream.read(magic);
      if (Arrays.equals(Packet.MAGIC_BYTES, magic)) {
        break;
      }
    }

    int byteCount = Packet.MAGIC_BYTES.length;

    Packet p = new Packet(0, 1);
    int b;
    while ((b = this.innerInputStream.read()) != -1) {
      p.data[byteCount++] = (byte) b;
      if (byteCount == Packet.BUFF) {
        break;
      }
    }

    return p;
  }

  private Packet wrappedReceivedPacket() {
    Packet p = new Packet(NO_SEQ, NO_ACK);
    try {
      p = this.receivePacket();
    } catch (Exception e) {
      this.globalExceptionFlag = true;
      this.lastException = e;
    }
    return p;
  }

  public CompletableFuture<Packet> timedReceivePacket() {
    return CompletableFuture.supplyAsync(this::wrappedReceivedPacket);
  }

  public Base32OutputStream getBase32OutputStream() {
    return this.innerOutputStream.getInnerWriter();
  }

  public void close() throws IOException {
    this.innerOutputStream.close();
    this.innerInputStream.close();
  }

  private int getNextSeq() {
    return ++this.seqCount;
  }

  private int getNextAck() {
    try {
      int ack = this.outGoingAckList.getFirst();
      return ack;
    } catch (NoSuchElementException e) {
      return NO_ACK;
    }
  }
}
