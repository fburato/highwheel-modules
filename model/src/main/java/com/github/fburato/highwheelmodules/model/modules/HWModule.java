package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

public final class HWModule implements MatchingModule {

    public final String name;
    private final AnonymousModule anonymousModule;

    private HWModule(String name, AnonymousModule anonymousModule) {
        this.name = name;
        this.anonymousModule = anonymousModule;
    }

    public static Optional<HWModule> make(String moduleName, String... globs) {
        return make(moduleName, Arrays.asList(globs));
    }

    public static Optional<HWModule> make(String moduleName, List<String> globs) {
        try {
            return AnonymousModule.make(globs).map(anonymousModule -> new HWModule(moduleName, anonymousModule));
        } catch (PatternSyntaxException e) {
            return Optional.empty();
        }
    }

    public boolean contains(ElementName elementName) {
        return anonymousModule.contains(elementName);
    }

    @Override
    public String toString() {
        return "HWModule{" + "name='" + name + '\'' + ", patternLiteral='" + anonymousModule.patternLiterals + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        HWModule module = (HWModule) o;

        return Objects.equals(this.name, module.name) && Objects.equals(this.anonymousModule, module.anonymousModule);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + anonymousModule.hashCode();
        return result;
    }
}
