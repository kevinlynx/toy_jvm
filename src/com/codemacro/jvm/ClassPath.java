package com.codemacro.jvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.freeinternals.format.FileFormatException;
import org.freeinternals.format.classfile.ClassFile;

/**
 * Created on 2017/2/18.
 */
public class ClassPath {
  private static Logger logger = Logger.getLogger(ClassPath.class.getName());
  private String[] mPathList;

  public ClassPath(String[] pathList) {
    mPathList = pathList;
  }

  public ClassFile loadClass(String fullName) {
    for (String path : mPathList) {
      ClassFile cf = loadClass(path, fullName);
      if (cf != null) {
        logger.info("success loaded class " + fullName + " from " + path);
        return cf;
      }
    }
    return null;
  }

  private ClassFile loadClass(String path, String fullName) {
    if (path.endsWith(".jar")) {
      return loadClassFromArchive(path, fullName);
    }
    String fileName = path + "/" + fullName + ".class";
    File file = new File(fileName);
    if (!file.exists() || !file.isFile()) {
      logger.fine("not found file in " + path);
      return null;
    }
    FileInputStream fs = null;
    byte[] data = new byte[(int)file.length()];
    try {
      fs = new FileInputStream(file);
      fs.read(data);
      return new ClassFile(data);
    } catch (FileNotFoundException e) {
      logger.severe(e.getMessage());
    } catch (IOException e) {
      logger.severe(e.getMessage());
    } catch (FileFormatException e) {
      logger.severe(e.getMessage());
    } finally {
      try {
        if (fs != null) {
          fs.close();
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, null, e);
      }
    }
    return null;
  }

  private ClassFile loadClassFromArchive(String path, String fullName) {
    logger.log(Level.WARNING, "not implemented");
    return null;
  }

  public static void main(String[] args) {
    ClassPath cp = new ClassPath(".;lib".split(";"));
    ClassFile cf = cp.loadClass("test/ClassFile");
    if (cf != null) {
      System.out.println(cf.getCPCount().getValue());
    }
  }
}
