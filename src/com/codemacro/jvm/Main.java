package com.codemacro.jvm;

import org.apache.commons.cli.*;

import java.util.function.Function;

/**
 * Created on 2017/2/23.
 */
public class Main {
  public static void run(String cp, String mainClass) {
    VM vm = new VM(cp.split(";"));
    vm.run(mainClass.replace('.', '/'));
  }

  public static void main(String[] args) {
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    options.addOption("cp", "classpath", true, "specify class path" );
    options.addOption("h", "help", false, "print this message" );
    Function<Void, Integer> pHelp = (v) -> {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "jvm", options);
      return -1;
    };
    try {
      String cp = "";
      CommandLine line = parser.parse(options, args);
      if( line.hasOption("classpath")) {
        cp = line.getOptionValue("classpath");
      }
      if (line.getArgList().size() == 0) {
        pHelp.apply(null);
        return;
      }
      String mainClass = line.getArgList().get(0);
      run(cp, mainClass);
    } catch(ParseException exp) {
      pHelp.apply(null);
    }
  }
}
