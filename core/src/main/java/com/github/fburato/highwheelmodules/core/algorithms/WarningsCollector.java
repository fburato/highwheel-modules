package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.HWModule;
import org.pitest.highwheel.model.ElementName;

import java.util.Collection;

public interface WarningsCollector {
  void constructionWarning(HWModule m);

  void accessPointWarning(ElementName elementName, Collection<HWModule> matchingModules);
}
