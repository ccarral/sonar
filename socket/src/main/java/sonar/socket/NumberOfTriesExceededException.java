package sonar.socket;

public class NumberOfTriesExceededException extends Exception {
  public String toString() {
    return "El número de intentos para recibir notificación de recepción del paquete ha sido"
               + " excedido.";
  }
}
