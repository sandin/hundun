package com.github.sandin.hundun.obfuscator;

import com.github.sandin.hundun.utils.OpcodeUtils;
import com.github.sandin.hundun.utils.SimpleNameGenerator;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;

import java.util.*;

public class RenameObfuscator extends ClassVisitor implements Opcodes {
    private static Map<String, SimpleNameGenerator> classNameGenerators = new HashMap<>();
    private SimpleNameGenerator fieldNameGenerator = new SimpleNameGenerator();
    private SimpleNameGenerator methodNameGenerator = new SimpleNameGenerator();

    private static Map<String, String> classNameMap = new HashMap<>(); // originName -> newName
    private static Map<String, String> fieldNameMap = new HashMap<>(); // originName -> newName
    private static Map<String, String> methodNameMap = new HashMap<>(); // originName -> newName

    private static Set<String> objectMethods = new HashSet<>();
    static {
        objectMethods.add("<init>");
        objectMethods.add("<clinit>");
        objectMethods.add("getClass");
        objectMethods.add("hashCode");
        objectMethods.add("equals");
        objectMethods.add("clone");
        objectMethods.add("toString");
        objectMethods.add("notify");
        objectMethods.add("notifyAll");
        objectMethods.add("wait");
        objectMethods.add("finalize");

        //objectMethods.add("main");
    }

    private String className;

    public RenameObfuscator(ClassVisitor classVisitor) {
        super(ASM9, classVisitor);
    }

    public String getNewClassName() {
        return classNameMap.get(className);
    }

    private String owner;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        owner = name;
        System.out.println("visit: " + name);

        String packageName = name.lastIndexOf("/") != -1 ? name.substring(0, name.lastIndexOf("/")) : "defaultPackage";
        SimpleNameGenerator classNameGenerator = classNameGenerators.get(packageName);
        if (classNameGenerator == null) {
            classNameGenerator = new SimpleNameGenerator();
            classNameGenerators.put(packageName, classNameGenerator);
        }
        String newName = packageName + "/" + classNameGenerator.nextName();
        className = name;
        classNameMap.put(name, newName);
        super.visit(version, access, newName, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        String newClassName = classNameMap.get(className);
        String newName = fieldNameGenerator.nextName();
        fieldNameMap.put(className + "." + name, newClassName + "." + newName);
        return super.visitField(access, newName, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("\nvisitMethod: " + name);
        if (!objectMethods.contains(name)) {
            String newClassName = classNameMap.get(className);
            String newName = methodNameGenerator.nextName();
            methodNameMap.put(className + "." + name + descriptor, newClassName + "." + newName + descriptor);
            name = newName;
        }

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new RenameMethodVisitor(owner, access, name, descriptor, mv);
    }

    //private class RenameMethodVisitor extends MethodVisitor {
    private class RenameMethodVisitor extends AnalyzerAdapter {
        private SimpleNameGenerator varNameGenerator = new SimpleNameGenerator();
        private int labelCount = 0;

        protected RenameMethodVisitor(String owner, int access, String name, String desc, MethodVisitor methodVisitor) {
            super(ASM9, owner, access, name, desc, methodVisitor);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            System.out.println("visitMaxs: maxLocals=" + maxLocals + ", maxStack=" + maxStack);
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            System.out.println("visitFrame: type=" + OpcodeUtils.frameType2str(type)
                    + ", numLocal=" + numLocal + ", local=" + OpcodeUtils.stackMapFrames2str(local)
                    + ", numStack=" + numStack + ", stack=" + OpcodeUtils.stackMapFrames2str(stack));
            super.visitFrame(type, numLocal, local, numStack, stack);
        }

        @Override
        public void visitLabel(Label label) {
            System.out.println("visitLabel, label=" + labelCount++);

            if (locals != null) {
                System.out.println("localVars: " + OpcodeUtils.stackMapFrames2str(locals.toArray()));
            }
            if (stack != null) {
                System.out.println("stack: " + OpcodeUtils.stackMapFrames2str(stack.toArray()));
            }
            super.visitLabel(label);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            System.out.println("visitLineNumber, line=" + line);
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (!"this".equals(name)) {
                name = varNameGenerator.nextName();

                Type type = Type.getType(descriptor); // TODO: array?
                String className = type.getInternalName();
                String newClassName = classNameMap.get(className);
                if (newClassName != null) {
                    type = Type.getObjectType(newClassName);
                    descriptor = type.getDescriptor();
                }
            }
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String newField = fieldNameMap.get(owner + "." + name);
            if (newField != null) {
                int idx = newField.lastIndexOf(".");
                owner = newField.substring(0, idx);
                name = newField.substring(idx + 1);
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            String newClassName = classNameMap.get(type);
            if (newClassName != null) {
                type = newClassName;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (objectMethods.contains(name)) {
                String newClassName = classNameMap.get(owner);
                if (newClassName != null) {
                    owner = newClassName;
                }
            } else {
                String newMethod = methodNameMap.get(owner + "." + name + descriptor);
                if (newMethod != null) {
                    int idx = newMethod.lastIndexOf(".");
                    owner = newMethod.substring(0, idx);
                    int idx2 = newMethod.lastIndexOf("(");
                    name = newMethod.substring(idx + 1, idx2);
                    descriptor = newMethod.substring(idx2);
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            super.visitVarInsn(opcode, varIndex);
        }
    }
}
