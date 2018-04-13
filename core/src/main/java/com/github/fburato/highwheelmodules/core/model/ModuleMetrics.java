package com.github.fburato.highwheelmodules.core.model;

import java.util.Optional;

public interface ModuleMetrics {
    Optional<Integer> fanInOf(Module module);
    Optional<Integer> fanOutOf(Module module);
}
