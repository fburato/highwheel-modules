package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.Collection;

public interface WarningsCollector {
  void constructionWarning(HWModule m);

  void accessPointWarning(ElementName elementName, Collection<HWModule> matchingModules);
}
