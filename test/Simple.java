package test;

public class Simple {
  private int data;

  public Simple() {
    Simple2 obj = new Simple2();
    int a = Simple2.inc(2);
  }

  public int add(int a, int b) {
    return a + b;
  }
}
