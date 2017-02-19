package com.codemacro.jvm;

import org.freeinternals.format.classfile.ClassFile;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created on 2017/2/18.
 */
public class ClassLoader {
  private static Logger logger = Logger.getLogger(ClassLoader.class.getName());
  private final ClassPath mCP;
  private final Map<String, Class> mClassTable;

  public ClassLoader(final ClassPath cp) {
    mCP = cp;
    mClassTable = new HashMap<>();
  }

  public Class loadClass(String fullName) {
    if (fullName.equals("java/lang/Object")) {
      logger.warning("ignore java/lang/Object");
      return null;
    }
    Class clazz = mClassTable.get(fullName);
    if (clazz != null) {
      return clazz;
    }
    ClassFile cf = mCP.loadClass(fullName);
    if (cf == null) {
      throw new RuntimeException("class not found:" + fullName);
    }
    clazz = newClass(cf);
    resolveSuperClass(clazz);
    resolveInterfaces(clazz);
    link(clazz);
    logger.info("loaded class " + fullName);
    mClassTable.put(fullName, clazz);
    return clazz;
  }

  private void resolveSuperClass(Class clazz) {
    // TODO
  }

  private void resolveInterfaces(Class clazz) {
    // TODO
  }

  private void link(Class clazz) {
    // TODO: verify
    prepare(clazz);
  }

  private void prepare(Class clazz) {

  }

  private Class newClass(ClassFile cf) {
    return new Class(cf, this);
  }
}
