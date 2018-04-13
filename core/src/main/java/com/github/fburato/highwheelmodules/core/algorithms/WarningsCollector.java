package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.Module;
import org.pitest.highwheel.model.ElementName;

import java.util.Collection;

public interface WarningsCollector {
  void constructionWarning(Module m);

  void accessPointWarning(ElementName elementName, Collection<Module> matchingModules);
}
