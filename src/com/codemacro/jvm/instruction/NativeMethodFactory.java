package com.codemacro.jvm.instruction;

import com.codemacro.jvm.Frame;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created on 2017/2/19.
 */
public class NativeMethodFactory {
  private static final Logger logger = Logger.getLogger(NativeMethodFactory.class.getName());
  private static final String CLZ_SYSTEM = "java/lang/System";
  interface NativeMethod {
    void exec(Frame frame);
  }

  private final Map<String, NativeMethod> mMethods;

  public NativeMethodFactory() {
    mMethods = new HashMap<>();
    mMethods.put(makeKey(CLZ_SYSTEM, "registerNative", "()V"), (frame) -> {
      registerAll();
    });
  }

  public void invoke(String clazzName, String methodName, String descriptor, Frame frame) {
    String key = makeKey(clazzName, methodName, descriptor);
    NativeMethod method = mMethods.get(key);
    if (method == null) {
      throw new RuntimeException("not found native method:" + key);
    }
    logger.info("call native method:" + key);
    method.exec(frame);
  }

  private void registerAll() {
    logger.info("register all native methods");
    mMethods.put(makeKey(CLZ_SYSTEM, "println", "(Ljava/lang/String;)V"), (frame) -> {
      String s = (String) frame.popRef();
      System.out.println(s);
    });
    mMethods.put(makeKey(CLZ_SYSTEM, "showVersion", "()V"), (frame) -> {
      System.out.println("toy jvm 0.0.1");
    });
    mMethods.put(makeKey("java/lang/StringBuilder", "append",
        "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"), (frame) -> {
      String s = (String) frame.popRef();
      Object data = frame.popRef();
      String ret = data == null ? s : ((String) data) + s;
      frame.pushRef(ret);
    });
    mMethods.put(makeKey("java/lang/String", "valueOf", "(I)Ljava/lang/String;"), (frame) -> {
      int i = frame.popInt();
      String s = String.valueOf(i);
      frame.pushRef(s); // invalid type for Slot
    });
  }

  private String makeKey(String clazzName, String methodName, String descriptor) {
    return clazzName + "@" + methodName + "@" + descriptor;
  }
}
