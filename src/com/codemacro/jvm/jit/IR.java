package com.codemacro.jvm.jit;

import org.freeinternals.format.classfile.Opcode;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IR to generate x86 like instructions
 */
public class IR {
  private static final Logger logger = Logger.getLogger(IR.class.getName());
  private static int inst_seed = 0x10;
  public static final int op_nop = 0x00;
  public static final int op_mov = inst(); // mov val, varM
  public static final int op_lod = inst(); // lod varN, varM
  public static final int op_mul = inst(); // mul varN, varM, ret -> varN
  public static final int op_div = inst();
  public static final int op_add = inst();
  public static final int op_sub = inst();
  public static final int op_jmp_gt = inst(); // jmp_gt var, #L
  public static final int op_jmp_ge = inst();
  public static final int op_jmp_eq = inst();
  public static final int op_jmp_ne = inst();
  public static final int op_jmp_lt = inst();
  public static final int op_jmp_le = inst();
  public static final int op_ret = inst(); // ret var
  public static final int op_label = inst(); // label #N

  private static int inst() { return inst_seed ++; }
  private static final Map<Integer, String> OP_STR = new HashMap<Integer, String>(){{
    put(op_mov, "mov");
    put(op_lod, "lod");
    put(op_mul, "mul");
    put(op_div, "div");
    put(op_add, "add");
    put(op_sub, "sub");
    put(op_jmp_gt, "jgt");
    put(op_jmp_ge, "jge");
    put(op_jmp_eq, "jeq");
    put(op_jmp_ne, "jne");
    put(op_jmp_lt, "jlt");
    put(op_jmp_le, "jle");
    put(op_ret, "ret");
    put(op_label, "lbl");
  }};

  public static class Inst {
    public int code = op_nop;
    public int op1 = 0, op2 = 0;

    public Inst(int code, int op1, int op2) { this.code = code; this.op1 = op1; this.op2 = op2; }
    public Inst(int code) { this.code = code; }
    public Inst(int code, int op1) {this.code = code; this.op1 = op1; }
  }

  private static class State {
    final List<Integer> labels;
    List<Inst> irs = new LinkedList<>();
    int maxLocals;
    int stackPos = 0;
    public State(final List<Integer> labels, int maxLocals) {
      this.labels = labels;
      this.maxLocals = maxLocals;
    }

    public State addIR(Inst i) { irs.add(i); return this; }
    public int pushStack() { return maxLocals + (stackPos ++); }
    public int popStack() { return maxLocals + (-- stackPos) ; }
    public int findLabel(int pc) { return labels.indexOf(pc); }
  }
  private static interface Translator {
    void translate(State state, InstParser.Instruction inst, Iterator<InstParser.Instruction> iterator);
  }
  private static final Map<Integer, Translator> translators = new HashMap<>();

  private interface LabelParser {
    int parse(InstParser.Instruction inst);
  }
  private static LabelParser ifLabelParser = (inst) -> {
    short offset = (short)((inst.op1 << 8) + inst.op2);
    return offset + inst.pc;
  };
  private static final Map<Integer, LabelParser> labelParsers = new HashMap<>();

  static {
    translators.put(Opcode.op_iconst_m1, createIConst(-1));
    translators.put(Opcode.op_iconst_0, createIConst(0));
    translators.put(Opcode.op_iconst_1, createIConst(1));
    translators.put(Opcode.op_iconst_2, createIConst(2));
    translators.put(Opcode.op_iconst_3, createIConst(3));
    translators.put(Opcode.op_iconst_4, createIConst(4));
    translators.put(Opcode.op_iconst_5, createIConst(5));

    translators.put(Opcode.op_istore_0, createIStore(0));
    translators.put(Opcode.op_istore_1, createIStore(1));
    translators.put(Opcode.op_istore_2, createIStore(2));
    translators.put(Opcode.op_istore_3, createIStore(3));

    translators.put(Opcode.op_iload_0, createILoad(0));
    translators.put(Opcode.op_iload_1, createILoad(1));
    translators.put(Opcode.op_iload_2, createILoad(2));
    translators.put(Opcode.op_iload_3, createILoad(3));

    translators.put(Opcode.op_imul, (state, inst, iterator) -> {
      int i1 = state.popStack();
      int i2 = state.popStack();
      state.addIR(new Inst(op_mul, i2, i1));
      state.pushStack();
    });
    translators.put(Opcode.op_isub, (state, inst, iterator) -> {
      int i1 = state.popStack();
      int i2 = state.popStack();
      state.addIR(new Inst(op_sub, i2, i1));
      state.pushStack();
    });

    translators.put(Opcode.op_ireturn, (state, inst, iterator) -> {
      int i = state.popStack();
      state.addIR(new Inst(op_ret, i));
    });

    translators.put(Opcode.op_ifgt, (state, inst, iterator) -> {
      short offset = (short)((inst.op1 << 8) + inst.op2);
      int pc = inst.pc + offset;
      int label = state.findLabel(pc);
      int var = state.popStack();
      state.addIR(new Inst(op_jmp_gt, var, label));
    });
    translators.put(Opcode.op_ifle, (state, inst, iterator) -> {
      short offset = (short)((inst.op1 << 8) + inst.op2);
      int pc = inst.pc + offset;
      int label = state.findLabel(pc);
      int var = state.popStack();
      state.addIR(new Inst(op_jmp_le, var, label));
    });

    labelParsers.put(Opcode.op_ifgt, ifLabelParser);
    labelParsers.put(Opcode.op_ifle, ifLabelParser);
  }

  private static Translator createIConst(int val) {
    return (state, inst, iterator) -> {
      int varIdx = state.pushStack();
      state.addIR(new Inst(op_mov, val, varIdx));
    };
  }

  private static Translator createIStore(int dstIdx) {
    return (state, inst, iterator) -> {
      int varIdx = state.popStack();
      state.addIR(new Inst(op_lod, varIdx, dstIdx));
    };
  }

  private static Translator createILoad(int frmIdx) {
     return (state, inst, iterator) -> {
      int varIdx = state.pushStack();
      state.addIR(new Inst(op_lod, frmIdx, varIdx));
    };
  }

  public static class IRResult {
    public final List<Inst> irs;
    public int maxLabel;
    public IRResult(List<Inst> irs, int maxLabel) {
      this.irs = irs;
      this.maxLabel = maxLabel;
    }
  }

  public IRResult generate(List<InstParser.Instruction> jbytecode, int maxLocals) {
    List<Integer> labels = createLabels(jbytecode);
    State state = new State(labels, maxLocals);
    Iterator<InstParser.Instruction> it = jbytecode.iterator();
    while (it.hasNext()) {
      InstParser.Instruction inst = it.next();
      int label = labels.indexOf(inst.pc);
      if (label >= 0) {
        state.addIR(new Inst(op_label, label));
      }
      Translator translator = translators.get(inst.opcode);
      if (translator == null) {
        logger.info(String.format("IR translate failed by not supported %02x", inst.opcode));
        return null;
      }
      translator.translate(state, inst, it);
    }
    dumpToFile(state.irs, "irs.out");
    return new IRResult(state.irs, labels.size());
  }

  public void writeTo(final List<Inst> insts, OutputStream s) throws IOException {
    DataOutputStream stream = new DataOutputStream(s);
    for (Inst inst : insts) {
      stream.writeByte(inst.code);
      stream.writeInt(inst.op1);
      stream.writeInt(inst.op2);
    }
  }

  public void dumpToFile(final List<Inst> insts, String name) {
    File file = new File(name);
    try {
      FileOutputStream stream = new FileOutputStream(file);
      writeTo(insts, stream);
      stream.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, "dump IRS to file failed", e);
    }
  }

  public void dump(final List<Inst> insts) {
    System.out.println(">> DUMP IRS <<");
    insts.forEach((inst) -> {
      String desc = OP_STR.get(inst.code);
      System.out.println(String.format("%s\t%d, %d", desc, inst.op1, inst.op2));
    });
  }

  private List<Integer> createLabels(List<InstParser.Instruction> jbytecode) {
    List<Integer> labels = new LinkedList<>();
    for (InstParser.Instruction i : jbytecode) {
      LabelParser labelParser = labelParsers.get(i.opcode);
      if (labelParser != null) {
        int pc = labelParser.parse(i);
        labels.add(pc);
      }
    }
    return labels;
  }
}
