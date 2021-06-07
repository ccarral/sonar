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

  public static final long DELAY_MS = 2300;

  private boolean checksumExceptionFlag;

  private boolean ioException;

  private NonMatchingChecksumException checksumExceptionHolder = null;

  private IOException ioExceptionHolder = null;

  private static final int RETRIES = 3;

  private int seqCount;

  // Activado cuando un ACK para un paquete no fue recibido despues de RETRIES
  private boolean timeout;

  // Lista en donde se guardan los Ack que vienen de regreso
  private LinkedList<Byte> incomingByteList;

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

    int byteCount = Packet.MAGIC_BYTES.length;

    Packet p = new Packet(NO_SEQ, NO_ACK);

    int b;
    while ((b = this.innerInputStream.read()) != -1) {
      p.data[byteCount++] = (byte) b;
      if (byteCount == Packet.BUFF) {
        break;
      }
    }

    // Verificar crc32
    CRC32 calculated = new CRC32();

    calculated.update(p.getSeq());
    calculated.update(p.getAck());
    calculated.update(p.data, Packet.HEADERS, p.getDataLength());

    if (calculated.getValue() != p.getCRC32()) {
      throw new NonMatchingChecksumException();
    }

    return p;
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

  // Recibe un paquete con timeout y escribe inmediatamente un Ack
  public Packet receiveLockstep(long timeout)
      throws IOException, TimeoutException, NonMatchingChecksumException, ExecutionException,
          InterruptedException {
    Packet received = this.timedReceivePacket(timeout);
    Packet ackPacket = new Packet(NO_SEQ, received.getSeq());
    this.writePacket(ackPacket);
    return received;
  }

  public void write(byte b) {
    if (this.currentPacket.getDataLength() == Packet.BUFF - Packet.HEADERS) {
      this.currentPacket = new Packet(NO_SEQ, NO_ACK);
    }
    this.currentPacket.write(b);
  }

  // Escribe el paquete actual, sin importarle si está lleno o no.
  // Intenta recibir un paquete de ACK correspondiente
  public void flush() throws NumberOfTriesExceededException {
    int tries = 0;
    while (true) {
      try {
        tries++;
        Packet received = this.writeLockstep(this.currentPacket, SonarSocket.DELAY_MS * 3);

        if (received.getAck() != currentPacket.getSeq()) {
          // Dios te ayude
          throw new Exception("El ack no coincide con el seq mandado.");
        }

      } catch (Exception e) {
        if (tries > SonarSocket.RETRIES) {
          throw new NumberOfTriesExceededException();
        }
      }
    }
  }

  // Bloquea hasta que termina la transmisión
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
