package com.github.sandin.hundun.core;

import com.github.sandin.hundun.obfuscator.RenameObfuscator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Hundun {
    public static final int LOG_INFO = 0;
    public static final int LOG_DEBUG = 1;
    public static final int LOG_VERBOSE = 2;

    private final int logLevel;
    private final CommandLine cmdLine;

    public Hundun(CommandLine options, int logLevel) {
        this.cmdLine = options;
        this.logLevel = logLevel;
    }

    public boolean debug() {
        return this.logLevel >= LOG_DEBUG;
    }

    public boolean verbose() {
        return this.logLevel >= LOG_VERBOSE;
    }

    public boolean obfuscate() throws HundunException {
        boolean ret = true;
        String classNameFilter = cmdLine.getOptionValue("filter");
        Pattern classFilterPattern = classNameFilter != null ? Pattern.compile(classNameFilter) : null;

        String inputZipFile = cmdLine.getOptionValue("input");
        String outputZipFile = cmdLine.getOptionValue("output");

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputZipFile)));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipFile))
        ) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = IOUtils.toByteArray(zis);
                String name = entry.getName();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().substring(0, entry.getName().length() - ".class".length())
                            .replaceAll("\\.", "/");
                    if (classFilterPattern == null || classFilterPattern.matcher(className).hasMatch()) {
                        ClassData r = modifyClassData(new ClassData(className, data));
                        data = r.data;
                        name = r.className + ".class";
                    }
                }

                ZipEntry newEntry = new ZipEntry(name);
                zos.putNextEntry(newEntry);
                zos.write(data);
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new HundunException("can not parse the input zip file!", e);
        }
        return ret;
    }

    public static class ClassData {
        public String className;
        public byte[] data;

        public ClassData(String className, byte[] data) {
            this.className = className;
            this.data = data;
        }
    }

    private ClassData modifyClassData(ClassData data) {
        /*
        ClassWriter cw = new ClassWriter(0);
        ClassReader cr = new ClassReader(data);
        cr.accept(cw, 0);
        byte[] b2 = cw.toByteArray(); // b2 represents the same class as b1
        */
        if (debug()) {
            System.out.println("modify Class: " + data.className);
        }

        ClassData ret = new ClassData(data.className, data.data);
        ClassReader cr = new ClassReader(data.data);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = cw;
        if (verbose()) {
            cv = new TraceClassVisitor(cw, new PrintWriter(System.out));
        }
        if (cmdLine.hasOption("verify")) {
            cv = new CheckClassAdapter(cv);
        }

        RenameObfuscator ca = new RenameObfuscator(cv);
        cr.accept(ca, ClassReader.EXPAND_FRAMES);
        ret.data = cw.toByteArray();
        ret.className = ca.getNewClassName();

        return ret;
    }
}
