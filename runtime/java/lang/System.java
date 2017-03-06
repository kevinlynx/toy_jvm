package java.lang;

public class System {
  public static native void println(String s);
  public static native void showVersion();
  public static native int currentTimeSeconds();
  private static native void registerNative();

  static {
    registerNative();
  }
}
