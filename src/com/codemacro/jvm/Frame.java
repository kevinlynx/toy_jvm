package com.codemacro.jvm;

import com.codemacro.jvm.instruction.InstructionFactory;
import org.freeinternals.format.classfile.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created on 2017/2/18.
 */
public class Frame {
  private static final Logger logger = Logger.getLogger(Frame.class.getName());
  private final Thread mThread;
  private final Class mClazz;
  private final MethodInfo mMethod;
  private int mPC = 0;
  private Slot[] mLocals;
  private Slot[] mOperStacks;
  private int mStackPos;
  private PosDataInputStream mCodeStream;

  public Frame(final Thread thread, final Class clazz, final MethodInfo method) {
    mThread = thread;
    mClazz = clazz;
    mMethod = method;
    initialize();
  }

  public void run() {
    InstructionFactory.Instruction inst = InstructionFactory.createInstruction(mCodeStream);
    try {
      mPC = mCodeStream.getPos();
      inst.exec(mCodeStream, this);
    } catch (IOException e) {
      throw new RuntimeException("load op value failed", e);
    }
  }

  public void pushInt(int i) {
    mOperStacks[mStackPos++] = new Slot(i);
  }

  public int popInt() {
    mStackPos --;
    return mOperStacks[mStackPos].i;
  }

  public void pushRef(Object ref) {
    mOperStacks[mStackPos++] = new Slot(ref);
  }

  public void pushSlot(Slot s) {
    mOperStacks[mStackPos++] = s;
  }

  public Object popRef() {
    mStackPos --;
    return mOperStacks[mStackPos].obj;
  }

  public Slot popSlot() {
    mStackPos --;
    return mOperStacks[mStackPos];
  }

  public void storeLocal(int i, int v) {
    mLocals[i] = new Slot(v);
  }

  public void storeLocal(int i, Object ref) {
    mLocals[i] = new Slot(ref);
  }

  public void storeLocal(int i, Slot slot) {
    mLocals[i] = slot;
  }

  public int loadLocal(int i) {
    return mLocals[i].i;
  }

  public Object loadRefLocal(int i) {
    return mLocals[i].obj;
  }

  public Thread getThread() {
    return mThread;
  }
  public Class getClazz() {
    return mClazz;
  }
  public AttributeCode.ExceptionTable getExceptionTable(int idx) { return getCode().getExceptionTable(idx); }
  public int getExceptionTableLength() { return getCode().getExceptionTableLength(); }

  public int getPC() { return mPC; }

  public void setPC(int pc) {
    int offset = pc - mPC + 1; // offset OpCode
    offsetPC(offset);
  }

  public void offsetPC(int offset) {
    int pc = mPC - 1;
    if (offset < 0) {
      try {
        mCodeStream.reset();
        offset = pc + offset;
      } catch (IOException e) {
        throw new RuntimeException("reset code stream failed", e);
      }
    } else {
      offset = offset - (mCodeStream.getPos() - mPC) - 1;
    }
    try {
      mCodeStream.skip(offset);
    } catch (IOException e) {
      throw new RuntimeException("offset PC exception", e);
    }
  }

  public void dump() {
    logger.info("Dump frame ==> " + getName());
    logger.info("Local Variables:");
    String line = "";
    for (Slot s : mLocals) {
      if (s != null) { // `main' function
        line = line + (s.toString() + " ");
      }
    }
    logger.info(line);
    if (mStackPos > 0) {
      logger.info("Stack:");
      line = "";
      for (int i = 0; i < mStackPos; ++i) {
        line = line + (mOperStacks[i].toString() + " ");
      }
      logger.info(line);
    } else {
      logger.info("Stack: <Empty>");
    }
  }

  public String getName() {
    return mClazz.getNameInConstantPool(mMethod.getNameIndex());
  }

  private void initialize() {
    mPC = 0;
    AttributeCode attr = getCode();
    if (attr == null) {
      throw new RuntimeException("not found code attribute");
    }
    mLocals = new Slot [attr.getMaxLocals()];
    mOperStacks = new Slot [attr.getMaxStack()];
    mStackPos = 0;
    mCodeStream = new PosDataInputStream(new PosByteArrayInputStream(attr.getCode()));
    try {
      mCodeStream.mark(mCodeStream.available()); // so that we can reset to the beginning
    } catch (IOException e) {
      logger.log(Level.SEVERE, null, e);
    }
  }

  private AttributeCode getCode() {
    return (AttributeCode) getAttribute(AttributeInfo.TypeCode);
  }

  private AttributeInfo getAttribute(String name) {
    for (int i = 0; i < mMethod.getAttributesCount(); ++i) {
      AttributeInfo attr = mMethod.getAttribute(i);
      if (attr.getName().equals(name)) {
        return attr;
      }
    }
    return null;
  }
}
