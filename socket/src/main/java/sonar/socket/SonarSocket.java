package sonar.socket;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import org.apache.commons.codec.binary.Base32OutputStream;
import sonar.minimodem.*;

public class SonarSocket {

  private BufferedTransmitter innerOutputStream;
  private BufferedReceiver innerInputStream;

  private static final int NO_SEQ = 0;
  private static final int NO_ACK = 1;

  public static final long DELAY_MS = 3000;

  private boolean eofFlag;

  private boolean checksumExceptionFlag;

  private boolean ioException;

  private NonMatchingChecksumException checksumExceptionHolder = null;

  private IOException ioExceptionHolder = null;

  private static final int RETRIES = 5;

  private int seqCount;

  // Activado cuando un ACK para un paquete no fue recibido despues de RETRIES
  private boolean timeout;

  // Lista en donde se guardan los Ack que vienen de regreso
  public LinkedList<Byte> incomingByteList;

  // Lista de donde se obtienen los números de ack siguientes
  private LinkedList<Integer> outGoingAckList;

  // Lista de Packets que no han recibido ack de vuelta
  private LinkedList<Packet> outgoingPackets;

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

    this.eofFlag = false;

    this.checksumExceptionFlag = false;

    this.ioException = false;

    this.outGoingAckList = new LinkedList<Integer>();

    this.incomingByteList = new LinkedList<Byte>();

    this.outgoingPackets = new LinkedList<Packet>();

    this.tries = Collections.synchronizedMap(new HashMap<Integer, Integer>());

    this.defaultScheduledExecutor = new ScheduledThreadPoolExecutor(2);

    this.currentPacket = new Packet(this.getNextSeq(), this.getNextAck());
  }

  public void writePacket(Packet packet) throws IOException {
    if (this.eofFlag) {
      packet.setEOF(true);
    }
    for (int i = 0; i < Packet.BUFF; i++) {
      this.innerOutputStream.write(packet.data[i]);
      this.innerOutputStream.flush();
    }

    this.innerOutputStream.getInnerWriter().close();
  }

  public Packet receivePacket() throws IOException, NonMatchingChecksumException {

    // Detectar número mágico
    byte[] magic = new byte[4];

    while (true) {
      this.innerInputStream.read(magic);
      if (Arrays.equals(Packet.MAGIC_BYTES, magic)) {
        break;
      }
    }

    byte[] buffer = new byte[Packet.BUFF];

    for (int i = 0; i < Packet.MAGIC_BYTES.length; i++) {
      buffer[i] = Packet.MAGIC_BYTES[i];
    }

    int byteCount = Packet.MAGIC_BYTES.length;

    int b;
    while ((b = this.innerInputStream.read()) != -1) {
      buffer[byteCount++] = (byte) b;
      if (byteCount == Packet.BUFF) {
        break;
      }
    }

    Packet p = new Packet(buffer);

    // Verificar crc32
    CRC32 calculated = new CRC32();

    calculated.update(p.getSeq());
    calculated.update(p.getAck());

    for (int i = Packet.HEADERS; i < Packet.HEADERS + p.getDataLength(); i++) {
      calculated.update(p.data[i]);
    }

    if (calculated.getValue() != p.getCRC32()) {
      throw new NonMatchingChecksumException();
    }

    return p;
  }

  // Bloquea hasta que se recibe la señal EOF
  // posteriormente todos los bytes están listos.
  public void receiveAllPackets()
      throws InterruptedException, ExecutionException, IOException, NonMatchingChecksumException,
          TimeoutException {
    boolean eof = false;
    do {
      Packet received = this.receiveLockstepNonStrict();
      System.out.println("Recibiendo un paquete");
      eof = received.getEOF();
      for (int i = Packet.HEADERS; i < Packet.HEADERS + received.getDataLength(); i++) {
        this.incomingByteList.add(received.data[i]);
      }

    } while (!eof);
  }

  private Packet wrappedReceivedPacket() {
    Packet p = new Packet(NO_SEQ, NO_ACK);
    try {
      p = this.receivePacket();
    } catch (NonMatchingChecksumException chksm) {
      this.checksumExceptionFlag = true;
      this.checksumExceptionHolder = chksm;
    } catch (IOException io) {
      this.ioException = true;
      this.ioExceptionHolder = io;
    }
    return p;
  }

  public CompletableFuture<Packet> timedReceivePacket0() {
    return CompletableFuture.supplyAsync(this::wrappedReceivedPacket);
  }

  public Packet timedReceivePacket(long timeout)
      throws ExecutionException, InterruptedException, TimeoutException, IOException,
          NonMatchingChecksumException {
    Future<Packet> future = timedReceivePacket0();
    Packet p = future.get(timeout, TimeUnit.MILLISECONDS);
    if (this.checksumExceptionFlag) {
      throw this.checksumExceptionHolder;
    }
    if (this.ioException) {
      throw this.ioExceptionHolder;
    }

    return p;
  }

  // Escribe un paquete y recibe un paquete
  public Packet writeLockstep(Packet p, long timeout)
      throws IOException, ExecutionException, InterruptedException, TimeoutException,
          NonMatchingChecksumException {
    this.writePacket(p);
    Packet received = this.timedReceivePacket(timeout);
    return received;
  }

  public Packet writeLockstepNonStrict(Packet p) throws IOException, NonMatchingChecksumException {
    this.writePacket(p);
    Packet received = this.receivePacket();
    return received;
  }

  // Recibe un paquete con timeout y escribe inmediatamente un Ack
  public Packet receiveLockstep(long timeout)
      throws IOException, TimeoutException, NonMatchingChecksumException, ExecutionException,
          InterruptedException {
    Packet received = this.timedReceivePacket(timeout);
    Packet ackPacket = new Packet(NO_SEQ, received.getSeq());
    this.writePacket(ackPacket);
    return received;
  }

  public Packet receiveLockstepNonStrict() throws IOException, NonMatchingChecksumException {
    Packet received = this.receivePacket();
    Packet ackPacket = new Packet(NO_SEQ, received.getSeq());
    this.writePacket(ackPacket);
    return received;
  }

  public void write(byte b) throws NumberOfTriesExceededException {
    // System.out.println("Inside write(): " + this.currentPacket.getDataLength());
    if (this.currentPacket.getDataLength() == Packet.BUFF - Packet.HEADERS) {
      this.flush();
      this.currentPacket = new Packet(this.getNextSeq(), this.getNextAck());
    }
    this.currentPacket.write(b);
  }

  // Escribe el paquete actual, sin importarle si está lleno o no.
  // Intenta recibir un paquete de ACK correspondiente
  public void flush() throws NumberOfTriesExceededException {
    int tries = 0;
    System.out.println("Inside flush");
    while (tries < SonarSocket.RETRIES) {
      System.out.println("Hola");
      try {
        tries++;
        Packet received = this.writeLockstep(this.currentPacket, SonarSocket.DELAY_MS * 3);
        System.out.println("flushed");

        if (received.getAck() != currentPacket.getSeq()) {
          // Dios te ayude
          throw new Exception("El ack no coincide con el seq mandado.");
        }

        break;

      } catch (Exception e) {
        System.err.println("Error durante el flush");
        System.err.println(e.toString());
        if (tries == SonarSocket.RETRIES) {
          System.err.println(e.toString());
          throw new NumberOfTriesExceededException();
        }
      }
    }
  }

  // Activa internamente la bandera eof para los paquetes que  envía (y recibe).
  public void signalEOF() {
    this.eofFlag = true;
  }

  // Bloquear hasta que se recibe la bandera EOF
  public byte read() {
    return 0x0;
  }

  public void addToOutgoingQueue(Packet p) {
    this.outgoingPackets.addLast(p);
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
