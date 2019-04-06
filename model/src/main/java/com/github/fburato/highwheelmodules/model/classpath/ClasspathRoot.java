package com.github.fburato.highwheelmodules.model.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public interface ClasspathRoot {

    InputStream getData(ElementName name) throws IOException;

    Collection<ElementName> classNames();

    InputStream getResource(final String name) throws IOException;
}
