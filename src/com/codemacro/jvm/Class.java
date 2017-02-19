package com.codemacro.jvm;

import org.freeinternals.format.FileFormatException;
import org.freeinternals.format.classfile.*;

import java.util.logging.Logger;

/**
 * Created on 2017/2/18.
 */
public class Class {
  private static final Logger logger = Logger.getLogger(Class.class.getName());
  private final ClassFile mCF;
  private final ClassLoader mClassLoader;
  private boolean mStaticInited = false;

  public static class Symbol {
    public Class clazz;
    public MethodInfo method;
    public int argCnt;
    public Symbol(Class clazz, MethodInfo method, int argCnt) {
      this.clazz = clazz;
      this.method = method;
      this.argCnt = argCnt;
    }
  }

  public Class(final ClassFile cf, final ClassLoader loader) {
    mCF = cf;
    mClassLoader = loader;
  }

  public boolean isStaticInited() { return mStaticInited; }
  public void setStaticInited() { mStaticInited = true; }

  public boolean isPublic() {
    return (mCF.getAccessFlags().getValue() & AccessFlags.ACC_PUBLIC) != 0;
  }

  public String getName() {
    return resolveClassName(mCF.getThisClass().getValue());
  }

  public MethodInfo findMethod(String name, String descriptor) {
    for (MethodInfo m : mCF.getMethods()) {
      String methodName = getNameInConstantPool(m.getNameIndex());
      String methodDesc = getNameInConstantPool(m.getDescriptorIndex());
      if (methodName.equals(name) && methodDesc.equals(descriptor)) {
        logger.info("found method " + name + ":" + descriptor);
        return m;
      }
    }
    return null;
  }

  public FieldInfo findField(String name) {
    for (FieldInfo field : mCF.getFields()) {
      String fname = getNameInConstantPool(field.getNameIndex());
      if (fname.equals(name)) {
        return field;
      }
    }
    throw new RuntimeException("not found field:" + name);
  }

  public String resolveFieldName(int idx) {
    AbstractCPInfo[] pool = mCF.getConstantPool();
    ConstantFieldrefInfo info = (ConstantFieldrefInfo) pool[idx];
    ConstantNameAndTypeInfo nameAndType = (ConstantNameAndTypeInfo) pool[info.getNameAndTypeIndex()];
    String fieldName = getNameInConstantPool(nameAndType.getNameIndex());
    return fieldName;
  }

  public Symbol resolveMethodByRef(int poolIdx) {
    AbstractCPInfo[] pool = mCF.getConstantPool();
    AbstractCPInfo info = pool[poolIdx];
    assert(info.getTag() == AbstractCPInfo.CONSTANT_Methodref);
    ConstantMethodrefInfo refInfo = (ConstantMethodrefInfo) info;
    int clazzIdx = refInfo.getClassIndex();
    ConstantClassInfo clazzInfo = (ConstantClassInfo) pool[clazzIdx];
    String clazzName = getNameInConstantPool(clazzInfo.getNameIndex());
    // load class, maybe the class is this
    Class clazz = mClassLoader.loadClass(clazzName);
    if (clazz == null) {
      return null;
    }
    int nameTypeIdx = refInfo.getNameAndTypeIndex();
    // load method name and prototype descriptor
    ConstantNameAndTypeInfo nameAndType = (ConstantNameAndTypeInfo) pool[nameTypeIdx];
    String methodName = getNameInConstantPool(nameAndType.getNameIndex());
    String descriptor = getNameInConstantPool(nameAndType.getDescriptorIndex());
    // find the real method by name and descriptor
    MethodInfo method = clazz.findMethod(methodName, descriptor);
    return new Symbol(clazz, method, parseArgCount(method, descriptor));
  }

  public String resolveClassName(int idx) {
    AbstractCPInfo[] pool = mCF.getConstantPool();
    ConstantClassInfo clazzInfo = (ConstantClassInfo) pool[idx];
    String clazzName = getNameInConstantPool(clazzInfo.getNameIndex());
    return clazzName;
  }

  public ClassLoader getClassLoader() {
    return mClassLoader;
  }

  public String getNameInConstantPool(int idx) {
    try {
      return mCF.getConstantUtf8Value(idx);
    } catch (FileFormatException e) {
      throw new RuntimeException("not found Utf8 constant value at " + idx);
    }
  }

  public ClassFile getClassFile() {
    return mCF;
  }

  // TODO: handle float & double type
  private int parseArgCount(MethodInfo method, String descriptor) {
    int i1 = descriptor.indexOf('(');
    int i2 = descriptor.indexOf(')');
    int cnt = (method.getAccessFlags() & MethodInfo.ACC_STATIC) == 0 ? 1 : 0;
    for (int i = i1 + 1; i < i2; ++i, ++cnt) {
      if (descriptor.charAt(i) == 'L') {
        i = descriptor.indexOf(';', i + 1);
      }
    }
    return cnt;
  }

}
