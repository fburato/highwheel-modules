package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.utils.Builder;
import com.github.fburato.highwheelmodules.utils.Pair;

import java.util.*;

public interface AnalyserModel {

    class Metrics {
        public final String module;
        public final int fanIn;
        public final int fanOut;

        public Metrics(String module, int fanIn, int fanOut) {
            this.module = module;
            this.fanIn = fanIn;
            this.fanOut = fanOut;
        }

        @Override
        public String toString() {
            return "Metrics{" + "module='" + module + '\'' + ", fanIn=" + fanIn + ", fanOut=" + fanOut + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Metrics that = (Metrics) o;

            return Objects.equals(this.module, that.module) && Objects.equals(this.fanIn, that.fanIn)
                    && Objects.equals(this.fanOut, that.fanOut);
        }

        @Override
        public int hashCode() {
            int result = module != null ? module.hashCode() : 0;
            result = 31 * result + fanIn;
            result = 31 * result + fanOut;
            return result;
        }
    }

    class EvidenceBackedViolation {
        public final String sourceModule;
        public final String destinationModule;
        public final List<String> specificationPath;
        public final List<String> actualPath;
        public final List<List<Pair<String, String>>> evidences;

        public EvidenceBackedViolation(String sourceModule, String destinationModule, List<String> specificationPath,
                List<String> actualPath, List<List<Pair<String, String>>> evidences) {
            this.sourceModule = sourceModule;
            this.destinationModule = destinationModule;
            this.specificationPath = specificationPath;
            this.actualPath = actualPath;
            this.evidences = evidences;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            EvidenceBackedViolation that = (EvidenceBackedViolation) o;
            return Objects.equals(sourceModule, that.sourceModule)
                    && Objects.equals(destinationModule, that.destinationModule)
                    && Objects.equals(specificationPath, that.specificationPath)
                    && Objects.equals(actualPath, that.actualPath) && Objects.equals(evidences, that.evidences);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceModule, destinationModule, specificationPath, actualPath, evidences);
        }

        @Override
        public String toString() {
            return "EvidenceBackedViolation{" + "sourceModule='" + sourceModule + '\'' + ", destinationModule='"
                    + destinationModule + '\'' + ", specificationPath=" + specificationPath + ", actualPath="
                    + actualPath + ", evidences=" + evidences + '}';
        }
    }

    class ModuleConnectionViolation {
        public final String sourceModule;
        public final String destinationModule;

        public ModuleConnectionViolation(String sourceModule, String destinationModule) {
            this.sourceModule = sourceModule;
            this.destinationModule = destinationModule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ModuleConnectionViolation that = (ModuleConnectionViolation) o;
            return Objects.equals(sourceModule, that.sourceModule)
                    && Objects.equals(destinationModule, that.destinationModule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceModule, destinationModule);
        }

        @Override
        public String toString() {
            return "ModuleConnectionViolation{" + "sourceModule='" + sourceModule + '\'' + ", destinationModule='"
                    + destinationModule + '\'' + '}';
        }
    }

    class AnalysisResult {
        public final Collection<EvidenceBackedViolation> evidenceBackedViolations;
        public final Collection<ModuleConnectionViolation> moduleConnectionViolations;
        public final Collection<Metrics> metrics;

        public AnalysisResult(Collection<EvidenceBackedViolation> evidenceBackedViolations,
                Collection<ModuleConnectionViolation> moduleConnectionViolations, Collection<Metrics> metrics) {
            this.evidenceBackedViolations = evidenceBackedViolations;
            this.moduleConnectionViolations = moduleConnectionViolations;
            this.metrics = metrics;
        }
    }
}
