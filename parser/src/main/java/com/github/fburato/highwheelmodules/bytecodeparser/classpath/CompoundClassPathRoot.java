package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

/**
 * Wraps multiple child roots.
 */
public class CompoundClassPathRoot implements ClasspathRoot {

    private final List<ClasspathRoot> roots;

    public CompoundClassPathRoot(final List<ClasspathRoot> roots) {
        this.roots = roots;
    }

    public InputStream getData(final ElementName name) throws IOException {
        for (final ClasspathRoot each : this.roots) {
            final InputStream is = each.getData(name);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    public Collection<ElementName> classNames() {
        final List<ElementName> cns = new ArrayList<>();
        for (final ClasspathRoot each : this.roots) {
            cns.addAll(each.classNames());
        }
        return cns;
    }

    public InputStream getResource(final String name) throws IOException {
        for (final ClasspathRoot each : this.roots) {
            final InputStream is = each.getResource(name);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

}
