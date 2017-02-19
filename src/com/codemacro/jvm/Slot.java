package com.codemacro.jvm;

/**
 * Created on 2017/2/19.
 */
public class Slot {
  public enum Type {REF, NUM};
  public Type type;
  public int i = 0;
  public Object obj = null;

  public Slot(int i) {
    this.i = i;
    this.type = Type.NUM;
  }

  public Slot(Object obj) {
    this.obj = obj;
    this.type = Type.REF;
  }

  public String toString() {
    return this.type == Type.NUM ? String.format("0x%04x", i) : obj.toString();
  }
}
