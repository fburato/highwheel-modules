package com.github.fburato.highwheelmodules.model.classpath;

import java.io.IOException;

public interface ClassParser {

   void parse(ClasspathRoot cp, final AccessVisitor v) throws IOException;
	
}
