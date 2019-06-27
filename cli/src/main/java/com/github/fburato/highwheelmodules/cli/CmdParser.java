package com.github.fburato.highwheelmodules.cli;

import com.github.fburato.highwheelmodules.core.AnalyserFacade;
import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CmdParser {
    private final Options options = new Options();
    private final Option spec = Option.builder("s").longOpt("specification").optionalArg(true).hasArg(true)
            .desc("Path to the specification file").build();
    private final Option limitOpt = Option.builder("l").longOpt("evidenceLimit").optionalArg(true).hasArg(true)
            .desc("Limit to the amount of evidence collected. Must be an integer").build();

    public final List<String> specificationFiles;
    public final Optional<Integer> evidenceLimit;
    public final List<String> argList;

    public CmdParser(String[] argv) {
        options.addOption(spec).addOption(limitOpt);

        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, argv);
        } catch (ParseException e) {
            throw new CliException("Error while parsing the command line arguments: " + e.getMessage());
        }
        String[] specOptions = cmd.getOptionValues("specification");
        List<String> specificationPath;
        if (specOptions == null) {
            specificationPath = Collections.singletonList("spec.hwm");
        } else {
            specificationPath = Arrays.asList(specOptions);
        }
        String limit = cmd.getOptionValue("evidenceLimit");
        try {
            if (limit == null) {
                evidenceLimit = Optional.empty();
            } else {
                evidenceLimit = Optional.of(Integer.parseInt(limit));
            }
        } catch (NumberFormatException nfe) {
            throw new CliException(String.format("'%s' is not an integer", limit));
        }
        specificationFiles = specificationPath;
        argList = cmd.getArgList();
    }
}
