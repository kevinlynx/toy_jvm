package com.codemacro.jvm.jit;

import org.freeinternals.format.classfile.PosByteArrayInputStream;
import org.freeinternals.format.classfile.PosDataInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created on 2017/3/5.
 */
public class JITMethodFactory {
  private static final Logger logger = Logger.getLogger(JITMethodFactory.class.getName());
  private static Map<String, ToyJIT> methods = new HashMap<>();
  private static InstParser bytecodeParser = new InstParser();
  private static IR irGenerator = new IR();
  public static boolean enable = false;

  public static ToyJIT compile(String clazzName, String methodName, String methodDescriptor,
                        final byte[] codes, int maxLocals, int maxStack, int argCnt, boolean hasRet) {
    if (!enable) return null;
    String k = key(clazzName, methodName, methodDescriptor);
    ToyJIT jit = methods.get(k);
    if (jit != null) {
      return jit;
    }
    jit = compile(codes, maxLocals, maxStack, argCnt, hasRet);
    if (jit != null) {
      logger.info(String.format("compile '%s' into native code", k));
      methods.put(k, jit);
    }
    return jit;
  }

  private static ToyJIT compile(final byte[] codes, int maxLocals, int maxStack, int argCnt, boolean hasRet) {
    PosDataInputStream stream = new PosDataInputStream(new PosByteArrayInputStream(codes));
    List<InstParser.Instruction> insts = bytecodeParser.parse(stream);
    IR.IRResult irRet = irGenerator.generate(insts, maxLocals);
    if (irRet == null) {
      return null;
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      irGenerator.writeTo(irRet.irs, output);
      ToyJIT jit = new ToyJIT();
      byte[] irdump = output.toByteArray();
      jit.initialize(irdump, maxLocals + maxStack, irRet.maxLabel, argCnt, hasRet ? 1 : 0);
      return jit;
    } catch (IOException e) {
      logger.log(Level.WARNING, "write IRS failed", e);
    }
    return null;
  }

  private static String key(String clazzName, String methodName, String methodDescriptor) {
    return clazzName + '#' + methodName + '#' + methodDescriptor;
  }
}
