package com.chris;


import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;

/**
 * 程序名称规范的编译器插件
 *
 * @author zhx
 * @date 2022/04/07
 */
public class NameChecker {

    private final Messager messager;

    NameCheckScanner nameCheckScanner = new NameCheckScanner();

    NameChecker(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
    }

    public void checkNames(Element element) {
        nameCheckScanner.scan(element);
    }

    private class NameCheckScanner extends ElementScanner8<Void, Void> {
        @Override
        public Void visitType(TypeElement e, Void unused) {
            scan(e.getTypeParameters(), unused);
            checkCamelCase(e, true);
            super.visitType(e, unused);
            return null;
        }

        @Override
        public Void visitVariable(VariableElement e, Void unused) {
            if (e.getKind() == ElementKind.ENUM_CONSTANT
                    || e.getConstantValue() != null
                    || heuristicallyConstant(e)) {
                checkAllCaps(e);
            } else {
                checkCamelCase(e, false);
            }
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement e, Void unused) {
            if (e.getKind() == ElementKind.METHOD) {
                Name name = e.getSimpleName();
                if (name.contentEquals(e.getEnclosingElement().getSimpleName())) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "method" + name + "should be prefixed with its class name", e);
                }
                checkCamelCase(e, false);
            }
            super.visitExecutable(e, unused);
            return null;
        }
    }

    private boolean heuristicallyConstant(VariableElement e) {
        if (e.getEnclosingElement().getKind() == ElementKind.INTERFACE) {
            return true;
        } else {
            return e.getKind() == ElementKind.FIELD;
        }
    }

    private void checkCamelCase(Element e, boolean initialCaps) {
        String name = e.getSimpleName().toString();
        boolean previousUpper = false;
        boolean conventional = true;
        int firstCodePoint = name.codePointAt(0);

        if (Character.isUpperCase(firstCodePoint)) {
            previousUpper = true;
            if (!initialCaps) {
                messager.printMessage(Diagnostic.Kind.WARNING, "name " + name + " should be start with lower case");
                return;
            }
        } else if (Character.isLowerCase(firstCodePoint)) {
            if (initialCaps) {
                messager.printMessage(Diagnostic.Kind.WARNING, "name " + name + " should start with an upper case letter");
                return;
            }
        } else {
            conventional = false;
        }

        if (conventional) {
            int cp = firstCodePoint;
            for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                cp = name.codePointAt(i);
                if (Character.isUpperCase(cp)) {
                    if (previousUpper) {
                        conventional = false;
                        break;
                    }
                    previousUpper = true;
                } else {
                    previousUpper = false;
                }
            }
        }

        if (!conventional) {
            messager.printMessage(Diagnostic.Kind.WARNING, "name " + name + " should be in camel case");
        }
    }

    private void checkAllCaps(Element e) {
        String name = e.getSimpleName().toString();
        boolean conventional = true;
        int firstCodePoint = name.codePointAt(0);

        if (Character.isUpperCase(firstCodePoint)) {
            conventional = false;
        } else {
            boolean previousUnderscore = false;
            int cp = firstCodePoint;
            for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                cp = name.codePointAt(i);
                if (cp == '_') {
                    if (previousUnderscore) {
                        conventional = false;
                        break;
                    }
                    previousUnderscore = true;
                } else {
                    previousUnderscore = false;
                    if (!Character.isUpperCase(cp) && !Character.isDigit(cp)) {
                        conventional = false;
                        break;
                    }
                }
            }
        }
        if (!conventional) {
            messager.printMessage(Diagnostic.Kind.WARNING, "name " + name + " should be all caps");
        }
    }
}

