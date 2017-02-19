package java.lang;

public class StringBuilder {
  Object data;

  public StringBuilder() {
    data = null;
  }

  public StringBuilder append(String s) {
    data = append(data, s);
    return this;    
  }

  public String toString() {
    return (String) data;
  }

  private static native Object append(Object data, String s);
}
