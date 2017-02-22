package test;

import java.lang.System;

public class ClassFile {
  private int data;
  private static int s_data;
  static {
    int a = 3;
    int b = simple(a);
  }
  public static class InnerStatic {
    public int mul(int a, int b) {
      return a * b;
    }
  }

  private class InnerNonStatic {
    public int div(int a, int b) {
      return a / b;
    }
  }

  public ClassFile() {
    data = 0x11;
  }

  public int add(int a, int b) {
    return a + b;
  }

  public int sub(int a, int b) {
    return a - b;
  }

  public static int fac(int n) {
    return n == 1 ? 1 : n * fac(n - 1);
  }

  public static int fac2(int n) {
    int r = 1;
    do {
      r = r * n;
      n = n - 1;
    } while (n > 0);
    return r;
  }

  public static int simple(int a) {
    return a + 2;
  }

  public static void testStaticFunc() {
    int a = fac(4);
    int b = fac2(4);
  }

  public static void testObject() {
    ClassFile obj = new ClassFile();
    obj.add(100, 200);
    int a = obj.data + 3;
  }

  public static void testString() {
    String s1 = "hello world";
    System.println(s1 + " from toy_jvm");
    System.println("static value: " + String.valueOf(101));
  }

  public static void funcThrow() {
    int a = 0;
    if (a == 0) throw new RuntimeException("exception message");
  }

  public static void testException() {
    try {
      funcThrow();
    } catch (RuntimeException e) {
      System.println("runtime exception");
    } finally {
      System.println("exception finally");
    } // */
  }

  public static void main(String[] args) {
    System.showVersion();
    testException();
  }
}