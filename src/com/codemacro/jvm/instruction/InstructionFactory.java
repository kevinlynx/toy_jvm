package com.codemacro.jvm.instruction;

import com.codemacro.jvm.Frame;
import com.codemacro.jvm.Class;
import com.codemacro.jvm.Slot;
import com.codemacro.jvm.VObject;
import org.freeinternals.format.classfile.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created on 2017/2/18.
 * https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings
 */
public class InstructionFactory {
  private static final Logger logger = Logger.getLogger(InstructionFactory.class.getName());
  public interface Instruction {
    void exec(final DataInputStream codes, final Frame frame) throws IOException;
  }

  private static NativeMethodFactory sNativeMethods;
  private static final Map<Integer, Instruction> instructions = new HashMap<>();

  static { initialize(); }

  public static Instruction createInstruction(DataInputStream stream) {
    try {
      int op = stream.readByte() & 0xff;
      Instruction inst = instructions.get(op);
      if (inst == null) {
        throw new RuntimeException(String.format("unsupported Opcode: 0x%02x", op));
      }
      return inst;
    } catch (IOException e) {
      logger.log(Level.SEVERE, null, e);
    }
    return null;
  }

  private static void initialize() {
    sNativeMethods = new NativeMethodFactory();

    register(Opcode.op_iconst_m1, createIConst(-1));
    register(Opcode.op_iconst_0, createIConst(0));
    register(Opcode.op_iconst_1, createIConst(1));
    register(Opcode.op_iconst_2, createIConst(2));
    register(Opcode.op_iconst_3, createIConst(3));
    register(Opcode.op_iconst_4, createIConst(4));
    register(Opcode.op_iconst_5, createIConst(5));

    register(Opcode.op_istore_0, createIStore(0));
    register(Opcode.op_istore_1, createIStore(1));
    register(Opcode.op_istore_2, createIStore(2));
    register(Opcode.op_istore_3, createIStore(3));
    register(Opcode.op_astore_0, createAStore(0));
    register(Opcode.op_astore_1, createAStore(1));
    register(Opcode.op_astore_2, createAStore(2));
    register(Opcode.op_astore_3, createAStore(3));

    register(Opcode.op_iload_0, createILoad(0));
    register(Opcode.op_iload_1, createILoad(1));
    register(Opcode.op_iload_2, createILoad(2));
    register(Opcode.op_iload_3, createILoad(3));
    register(Opcode.op_aload_0, createALoad(0));
    register(Opcode.op_aload_1, createALoad(1));
    register(Opcode.op_aload_2, createALoad(2));
    register(Opcode.op_aload_3, createALoad(3));

    register(Opcode.op_ldc, (codes, frame) -> {
      int idx = codes.readByte();
      AbstractCPInfo info = frame.getClazz().getClassFile().getConstantPool()[idx];
      int tag = info.getTag();
      if (tag == AbstractCPInfo.CONSTANT_String) {
        String val = frame.getClazz().getNameInConstantPool(((ConstantStringInfo) info).getStringIndex());
        frame.pushRef(val); // not good for slot because slot only holds Integer and VObject
      } else {
        throw new RuntimeException("not implemented ldc type");
      }
    });

    register(Opcode.op_bipush, (codes, frame) -> {
      frame.pushInt(codes.readByte());
    });
    register(Opcode.op_sipush, (codes, frame) -> {
      frame.pushInt(codes.readShort());
    });

    register(Opcode.op_dup, (codes, frame) -> {
      Slot s = frame.popSlot();
      frame.pushSlot(s);
      frame.pushSlot(s);
    });

    register(Opcode.op_pop, (codes, frame) -> {
      frame.popSlot();
    });

    register(Opcode.op_iinc, (codes, frame) -> {
      int v1 = codes.readByte();
      int i1 = codes.readByte();
      frame.storeLocal(v1, i1 + frame.loadLocal(v1));
    });
    register(Opcode.op_iadd, (codes, frame) -> {
      int i1 = frame.popInt();
      int i2 = frame.popInt();
      frame.pushInt(i1 + i2);
    });
    register(Opcode.op_isub, (codes, frame) -> {
      int i1 = frame.popInt();
      int i2 = frame.popInt();
      frame.pushInt(i2 - i1);
    });
    register(Opcode.op_imul, (codes, frame) -> {
      int i1 = frame.popInt();
      int i2 = frame.popInt();
      frame.pushInt(i1 * i2);
    });

    register(Opcode.op_return, (codes, frame) -> frame.getThread().popFrame());
    register(Opcode.op_ireturn, (codes, frame) -> {
      int i = frame.popInt();
      frame.getThread().popFrame();
      frame.getThread().topFrame().pushInt(i);
    });
    register(Opcode.op_areturn, (codes, frame) -> {
      Object ref = frame.popRef();
      frame.getThread().popFrame();
      frame.getThread().topFrame().pushRef(ref);
    });

    Function<Boolean, Instruction> createInvoke = (isStatic) -> {
      return (codes, frame) -> {
        int b1 = codes.readByte();
        int b2 = codes.readByte();
        int idx = (b1 << 8) + b2;
        Class.Symbol symbol = frame.getClazz().resolveMethodByRef(idx);
        if (symbol != null) {
          initClass(frame.getThread(), symbol.clazz);
        }
        // TODO: verify we can invoke the method
        invokeMethod(symbol, frame, isStatic);
      };
    };
    register(Opcode.op_invokestatic, createInvoke.apply(true));
    register(Opcode.op_invokevirtual, createInvoke.apply(false));
    register(Opcode.op_invokespecial, createInvoke.apply(false));

    register(Opcode.op_if_icmpne, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      int i1 = frame.popInt();
      int i2 = frame.popInt();
      if (i1 != i2) {
        frame.offsetPC(offset);
      }
    });
    register(Opcode.op_if_icmpge, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      int i1 = frame.popInt();
      int i2 = frame.popInt();
      if (i2 >= i1) {
        frame.offsetPC(offset);
      }
    });
    register(Opcode.op_ifgt, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      int i1 = frame.popInt();
      if (i1 > 0) {
        frame.offsetPC(offset);
      }
    });
    register(Opcode.op_ifle, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      int i1 = frame.popInt();
      if (i1 <= 0) {
        frame.offsetPC(offset);
      }
    });
    register(Opcode.op_ifne, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      int i1 = frame.popInt();
      if (i1 != 0) {
        frame.offsetPC(offset);
      }
    });

    register(Opcode.op_goto, (codes, frame) -> {
      int b1 = codes.readUnsignedByte();
      int b2 = codes.readUnsignedByte();
      short offset = (short)((b1 << 8) + b2);
      frame.offsetPC(offset);
    });

    register(Opcode.op_new, (codes, frame) -> {
      int b1 = codes.readByte();
      int b2 = codes.readByte();
      int idx = (b1 << 8) + b2;
      String clazzName = frame.getClazz().resolveClassName(idx);
      Class clazz = frame.getClazz().getClassLoader().loadClass(clazzName);
      initClass(frame.getThread(), clazz);
      VObject object = new VObject(clazz);
      frame.pushRef(object);
    });

    register(Opcode.op_putfield, (codes, frame) -> {
      int b1 = codes.readByte();
      int b2 = codes.readByte();
      int idx = (b1 << 8) + b2;
      String fieldName = frame.getClazz().resolveFieldName(idx);
      FieldInfo field = frame.getClazz().findField(fieldName);
      // TODO: verify if we can visit this field
      Slot val = frame.popSlot();
      VObject object = (VObject) frame.popRef();
      object.setField(fieldName, val);
    });

    register(Opcode.op_getfield, (codes, frame) -> {
      int b1 = codes.readByte();
      int b2 = codes.readByte();
      int idx = (b1 << 8) + b2;
      String fieldName = frame.getClazz().resolveFieldName(idx);
      VObject object = (VObject) frame.popRef();
      Slot val = object.getField(fieldName);
      frame.pushSlot(val);
    });

    register(Opcode.op_aconst_null, (codes, frame) -> {
      frame.pushRef(null);
    });

    register(Opcode.op_checkcast, (codes, frame) -> {
      int b1 = codes.readByte();
      int b2 = codes.readByte();
      int idx = (b1 << 8) + b2;
      String clazzName = frame.getClazz().resolveClassName(idx);
      Class clazz = frame.getClazz().getClassLoader().loadClass(clazzName);
      initClass(frame.getThread(), clazz);
      // TODO: do the real cast check
      Object ref = frame.popRef();
      frame.pushRef(ref);
    });

    register(Opcode.op_athrow, (codes, frame) -> {
      // TODO: clear the stack leaving only the exception object
      VObject ex = (VObject) frame.popRef();
      throwJump(ex, frame);
    });
  }

  private static Instruction createIConst(int val) {
    return (codes, frame) -> {
      frame.pushInt(val);
    };
  }

  private static Instruction createIStore(int idx) {
    return (codes, frame) -> {
      int val = frame.popInt();
      frame.storeLocal(idx, val);
    };
  }

  private static Instruction createAStore(int idx) {
    return (codes, frame) -> {
      Object ref = frame.popRef();
      frame.storeLocal(idx, ref);
    };
  }

  private static Instruction createILoad(int idx) {
    return (codes, frame) -> {
      int val = frame.loadLocal(idx);
      frame.pushInt(val);
    };
  }

  private static Instruction createALoad(int idx) {
    return (codes, frame) -> {
      Object ref = frame.loadRefLocal(idx);
      frame.pushRef(ref);
    };
  }

  private static void register(int op, Instruction inst) {
    instructions.put(op, (code, frame) -> {
      logger.info(String.format("exec 0x%02x", op));
      inst.exec(code, frame);
    });
  }

  private static void invokeMethod(Class.Symbol symbol, Frame frame, boolean isStatic) {
    if (symbol == null) { // maybe the class is java.lang.Object which not supported yet
      logger.warning("null symbol found, skip it");
      if (!isStatic) { // consume the instance argument at least (hack with java.lang.Object.<init>
        frame.popSlot();
      }
      return ;
    }
    if (callNative(symbol, frame)) {
      return;
    }
    Frame newFrame = new Frame(frame.getThread(), symbol.clazz, symbol.method);
    if (symbol.argCnt > 0) {
      for (int n = symbol.argCnt - 1; n >= 0; --n) { // TODO: handle float & double
        Slot val = frame.popSlot();
        newFrame.storeLocal(n, val);
      }
    }
    frame.getThread().pushFrame(newFrame);
  }

  private static boolean callNative(Class.Symbol symbol, Frame frame) {
    if ((symbol.method.getAccessFlags() & MethodInfo.ACC_NATIVE) != 0) {
      // TODO: verify access level
      String clazzName = symbol.clazz.getName();
      String methodName = symbol.clazz.getNameInConstantPool(symbol.method.getNameIndex());
      String descriptor = symbol.clazz.getNameInConstantPool(symbol.method.getDescriptorIndex());
      sNativeMethods.invoke(clazzName, methodName, descriptor, frame);
      return true;
    }
    return false;
  }

  private static void initClass(com.codemacro.jvm.Thread thread, Class clazz) {
    if (clazz.isStaticInited()) {
      return;
    }
    clazz.setStaticInited();
    MethodInfo method = clazz.findMethod("<clinit>", "()V");
    if (method != null) {
      Frame frame = new Frame(thread, clazz, method);
      thread.runToEnd(frame); // until this frame popped
      logger.info("class <clinit> finished");
    }
  }

  private static void throwJump(VObject ex, Frame frame) {
    while (frame.getThread().topFrame() != null) {
      frame.getThread().topFrame().pushRef(ex);
      int handler = findExceptionHandler(ex, frame.getThread().topFrame());
      if (handler >= 0) {
        logger.info("found exception handler at " + handler + " :" + frame.getThread().topFrame().getName());
        frame.getThread().topFrame().setPC(handler);
        return;
      }
      frame.getThread().popFrame();
    }
    throw new RuntimeException("no exception handler for:" + ex.getClazz().getName());
  }

  private static int findExceptionHandler(VObject ex, Frame frame) {
    int size = frame.getExceptionTableLength();
    if (size == 0) {
      logger.info("no exception table in " + frame.getName());
      return -1;
    }
    int pc = frame.getPC();
    for (int i = 0; i < size; ++i) {
      AttributeCode.ExceptionTable et = frame.getExceptionTable(i);
      if (et.getStartPc() <= pc && pc < et.getEndPc()) { // pc range matched
        // then check the exception type
        if (et.getCatchType() == 0) { // finally
          return et.getHandlerPc();
        } else {
          String exClassName = frame.getClazz().resolveClassName(et.getCatchType());
          // TODO: check inheritance
          if (ex.getClazz().getName().equals(exClassName)) {
            return et.getHandlerPc();
          }
        }
      } // pc range check
    } // for
    logger.info("not found exception handler in " + frame.getName());
    return -1;
  }
}
