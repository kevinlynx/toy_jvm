package com.codemacro.jvm;

import org.freeinternals.format.classfile.MethodInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created on 2017/2/18.
 */
public class Thread {
  private static Logger logger = Logger.getLogger(Thread.class.getName());
  private List<Frame> mFrames;

  public Thread() {
    mFrames = new LinkedList<>();
  }

  public void run(final Class clazz, final MethodInfo method) {
    Frame frame = new Frame(this, clazz, method);
    pushFrame(frame);
    while (!mFrames.isEmpty()) {
      topFrame().run();
    }
  }

  public void runToEnd(Frame startFrame) {
    pushFrame(startFrame);
    while (frameExist(startFrame)) {
      topFrame().run();
    }
  }

  public void pushFrame(Frame frame) {
    logger.info("push frame:" + frame.getName());
    mFrames.add(frame);
  }

  public void popFrame() {
    Frame frame = mFrames.remove(mFrames.size() - 1);
    logger.info("pop frame:" + frame.getName());
    frame.dump();
  }

  public Frame topFrame() {
    return mFrames.isEmpty() ? null : mFrames.get(mFrames.size() - 1);
  }

  public boolean frameExist(Frame frame) {
    return mFrames.lastIndexOf(frame) >= 0;
  }
}
