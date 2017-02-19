package com.codemacro.jvm;

import org.freeinternals.format.classfile.MethodInfo;

import java.util.function.Function;

/**
 * Created on 2017/2/18.
 */
public class VM {
  private ClassPath mCP;
  private ClassLoader mRootLoader;

  public VM(String[] pathList) {
    mCP = new ClassPath(pathList);
    mRootLoader = new ClassLoader(mCP);
  }

  public void run(String mainClass) {
    Class clazz = mRootLoader.loadClass(mainClass);
    // TODO: initialize the main class first
    MethodInfo method = clazz.findMethod("main", "([Ljava/lang/String;)V");
    Function<Integer, Boolean> hasFlag = (f) -> (f & method.getAccessFlags()) != 0;
    if (method == null || !hasFlag.apply(MethodInfo.ACC_PUBLIC) || !hasFlag.apply(MethodInfo.ACC_STATIC)) {
      throw new RuntimeException("main method is not static&public");
    }
    Thread thread = new Thread();
    thread.run(clazz, method);
  }

  public static void main(String[] args) {
    VM vm = new VM(".;lib".split(";"));
    vm.run("test/ClassFile");
  }
}
