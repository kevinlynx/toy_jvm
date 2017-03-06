package test;

import java.lang.System;

public class Simple3 {
  public static int fac2(int n) {
    int r = 1;
    do {
      r = r * n;
      n = n - 1;
    } while (n > 0);
    return r;
  }

  public static int sub(int n) {
    int r = n - 1;
    if (r > 0) return 1;
    return 0;
  }

  public static int le(int n) {
    if (n > 0) return 1;
    return 0;
  }

  public static void main(String[] args) {
    int start = System.currentTimeSeconds();
    System.println(String.valueOf(start));
    int i = 0;
    for (; i < 10000; ++i) {
        fac2(100);
    }
    int end = System.currentTimeSeconds();
    System.println(String.valueOf(i) + " times total used: " + String.valueOf((end - start)));
  }
}
