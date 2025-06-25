package com.github.sandin.hundun.core;

import com.github.sandin.hundun.obfuscator.RenameObfuscator;
import com.github.sandin.hundun.utils.SimpleNameGenerator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Hundun {
    private final CommandLine cmdLine;

    public Hundun(CommandLine options) {
        this.cmdLine = options;
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
                System.out.println("zipEntry: " + entry.getName());
                byte[] data = IOUtils.toByteArray(zis);
                String name = entry.getName();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().substring(0, entry.getName().length() - ".class".length())
                            .replaceAll("\\.", "/");
                    if (classFilterPattern == null || classFilterPattern.matcher(className).hasMatch()) {
                        ClassData r = modifyClassData(data);
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
    }

    private ClassData modifyClassData(byte[] data) {
        /*
        ClassWriter cw = new ClassWriter(0);
        ClassReader cr = new ClassReader(data);
        cr.accept(cw, 0);
        byte[] b2 = cw.toByteArray(); // b2 represents the same class as b1
        */

        ClassData ret = new ClassData();
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(cr, 0);
        RenameObfuscator ca = new RenameObfuscator(cw);
        cr.accept(ca, 0);
        ret.data = cw.toByteArray();
        ret.className = ca.getNewClassName();

        return ret;
    }
}
