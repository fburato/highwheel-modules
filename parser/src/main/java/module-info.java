module com.github.highwheel.modules.parser {
    requires com.github.highwheel.modules.utils;
    requires com.github.highwheel.modules.model;
    requires org.objectweb.asm;

    exports com.github.fburato.highwheelmodules.bytecodeparser;
    exports com.github.fburato.highwheelmodules.bytecodeparser.classpath;
}