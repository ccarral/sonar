package sonar.socket;

public class NonMatchingChecksumException extends Exception {
  public String toString() {
    return "CRC32 checksum doesn't match";
  }
}
