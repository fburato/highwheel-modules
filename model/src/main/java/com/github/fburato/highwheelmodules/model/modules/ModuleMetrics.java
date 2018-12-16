package com.github.fburato.highwheelmodules.model.modules;

import java.util.Optional;

public interface ModuleMetrics {
  Optional<Integer> fanInOf(HWModule module);

  Optional<Integer> fanOutOf(HWModule module);
}
