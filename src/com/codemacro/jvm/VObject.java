package com.codemacro.jvm;

import org.freeinternals.format.classfile.FieldInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2017/2/19.
 */
public class VObject {
  private Map<String, Slot> mFields;

  public VObject(final Class clazz) {
    mFields = new HashMap<>();
    initialize(clazz);
  }

  public void setField(String name, Slot slot) {
    mFields.put(name, slot);
  }

  public Slot getField(String name) {
    Slot slot = mFields.get(name);
    if (slot == null) {
      throw new RuntimeException("reference to uninitialized field:" + name);
    }
    return slot;
  }

  private void initialize(final Class clazz) {
    // should we initialize fields list here ?
    for (FieldInfo field : clazz.getClassFile().getFields()) {
      if (0 == (field.getAccessFlags() & FieldInfo.ACC_STATIC)) {

      }
    }
  }
}
