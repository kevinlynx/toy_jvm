package com.codemacro.jvm.jit;

import org.freeinternals.format.classfile.Opcode;
import org.freeinternals.format.classfile.PosDataInputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JVM Instruction parser, parse a byte array to a list of instructions including operands
 * 2017.03.03
 */
public class InstParser {
  private static final Logger logger = Logger.getLogger(InstParser.class.getName());
  public static class Instruction {
    public final int opcode;
    public final int pc;
    public int op1 = 0, op2 = 0;
    public enum Type { _0, _1, _2} ;
    Type type;
    public Instruction(int pc, int code) { this(pc, code, 0, 0, Type._0); }
    public Instruction(int pc, int code, int op1) { this(pc, code, op1, 0, Type._1); }
    public Instruction(int pc, int code, int op1, int op2) { this(pc, code, op1, op2, Type._2); }
    public Instruction(int pc, int code, int op1, int op2, Type t) {
      this.pc = pc;
      this.opcode = code;
      this.op1 = op1;
      this.op2 = op2;
      this.type = t;
    }
  }

  private interface Reader {
    Instruction read(int pc, int code, DataInputStream stream) throws IOException;
  }

  private Reader reader0 = (pc, code, stream) -> new Instruction(pc, code);
  private Reader readerB = (pc, code, stream) -> {
    int b1 = stream.readByte();
    return new Instruction(pc, code, b1);
  };
  private Reader readerBB = (pc, code, stream) -> {
    int b1 = stream.readByte();
    int b2 = stream.readByte();
    return new Instruction(pc, code, b1, b2);
  };
  private Reader readerS = (pc, code, stream) -> {
    int s1 = stream.readShort();
    return new Instruction(pc, code, s1);
  };
  private Reader readerI = (pc, code, stream) -> {
    int i1 = stream.readInt();
    return new Instruction(pc, code, i1);
  };
  private Reader readerUBUB = (pc, code, stream) -> {
    int b1 = stream.readUnsignedByte();
    int b2 = stream.readUnsignedByte();
    return new Instruction(pc, code, b1, b2);
  };
  private Map<Integer, Reader> readers = new HashMap<>();

  public InstParser() {
    register(Opcode.op_iconst_m1, reader0);
    register(Opcode.op_iconst_0, reader0);
    register(Opcode.op_iconst_1, reader0);
    register(Opcode.op_iconst_2, reader0);
    register(Opcode.op_iconst_3, reader0);
    register(Opcode.op_iconst_4, reader0);
    register(Opcode.op_iconst_5, reader0);

    register(Opcode.op_istore_0, reader0);
    register(Opcode.op_istore_1, reader0);
    register(Opcode.op_istore_2, reader0);
    register(Opcode.op_istore_3, reader0);
    register(Opcode.op_istore_3, reader0);

    register(Opcode.op_astore_0, reader0);
    register(Opcode.op_astore_1, reader0);
    register(Opcode.op_astore_2, reader0);
    register(Opcode.op_astore_3, reader0);

    register(Opcode.op_iload_0, reader0);
    register(Opcode.op_iload_1, reader0);
    register(Opcode.op_iload_2, reader0);
    register(Opcode.op_iload_3, reader0);
    register(Opcode.op_aload_0, reader0);
    register(Opcode.op_aload_1, reader0);
    register(Opcode.op_aload_2, reader0);
    register(Opcode.op_aload_3, reader0);

    register(Opcode.op_imul, reader0);
    register(Opcode.op_idiv, reader0);
    register(Opcode.op_iadd, reader0);
    register(Opcode.op_isub, reader0);

    register(Opcode.op_ifgt, readerUBUB);
    register(Opcode.op_ifle, readerUBUB);

    register(Opcode.op_ldc, readerB);
    register(Opcode.op_bipush, readerB);
    register(Opcode.op_sipush, readerS);

    register(Opcode.op_ireturn, reader0);
    register(Opcode.op_return, reader0);
    register(Opcode.op_invokestatic, readerBB);
    register(Opcode.op_pop, reader0);
  }

  public List<Instruction> parse(PosDataInputStream stream) {
    List<Instruction> insts = new LinkedList<>();
    try {
      while (stream.available() > 0) {
        int pc = stream.getPos();
        int code = stream.readByte() & 0xff;
        Reader reader = readers.get(code);
        if (reader == null) {
          throw new RuntimeException(String.format("not supported code 0x%02x at %04d", code, pc));
        }
        Instruction inst = reader.read(pc, code, stream);
        insts.add(inst);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, null, e);
    }
    dump(insts);
    return insts;
  }

  public void dump(List<Instruction> insts) {
    insts.forEach((inst) -> {
      System.out.println(String.format("%04d %02x %d %d", inst.pc, inst.opcode, inst.op1, inst.op2));
    });
  }

  private void register(int op, Reader reader) {
    readers.put(op, reader);
  }
}
