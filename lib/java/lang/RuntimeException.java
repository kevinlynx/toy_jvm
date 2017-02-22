package java.lang;

public class RuntimeException extends Throwable {
  private String msg;
  public RuntimeException(String msg) {
    this.msg = msg;
  }

  public String getMessage() {
    return msg;
  }
}

