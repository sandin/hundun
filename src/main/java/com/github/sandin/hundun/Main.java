package com.github.sandin.hundun;

import com.github.sandin.hundun.core.Hundun;
import com.github.sandin.hundun.core.HundunException;
import org.apache.commons.cli.*;

import java.awt.font.OpenType;
import java.io.File;

public class Main {
    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        // arguments
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("input").desc("input jar file").hasArg().required().build());
        options.addOption(Option.builder("o").longOpt("output").desc("output jar file").hasArg().required().build());
        options.addOption(Option.builder("f").longOpt("filter").desc("class name filter").hasArg().build());

        // parse arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = null;
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("hundun", options);
            System.exit(-1);
        }

        // check arguments
        if (!new File(cmdLine.getOptionValue("input")).exists()) {
            System.out.println(cmdLine.getOptionValue("input") + " file is not exists!");
            System.exit(-1);
        }

        Hundun hundun = new Hundun(cmdLine);
        try {
            hundun.obfuscate();
        } catch (HundunException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }
}