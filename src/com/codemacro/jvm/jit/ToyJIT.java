package com.codemacro.jvm.jit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * wrap libjit
 */
public class ToyJIT {
  private long jitPtr = 0;

  public void initialize(byte[] bytes, int maxLocals, int maxLabels, int argCnt, int retType) {
    jitPtr = compile(bytes, maxLocals, maxLabels, argCnt, retType);
  }

  public int invoke(int... args) {
    return invoke(jitPtr, args);
  }

  static {
    System.loadLibrary("toyjit");
  }
  private static native long compile(byte[] bytes, int maxLocals, int maxLabels, int argCnt, int retType);
  private static native int invoke(long jitPtr, int[] args);

  public static void main(String[] args) throws IOException {
    ToyJIT jit = new ToyJIT();
    Path path = (new File("irs.out")).toPath();
    byte[] data = Files.readAllBytes(path);
    jit.initialize(data, 4, 1, 1, 1);
    System.out.println(jit.invoke(3));
    System.out.println(jit.invoke(4));
  }
}
