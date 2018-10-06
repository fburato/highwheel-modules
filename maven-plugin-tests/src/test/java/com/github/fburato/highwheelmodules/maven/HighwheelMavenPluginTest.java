package com.github.fburato.highwheelmodules.maven;

import org.apache.maven.it.Verifier;
import org.junit.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class HighwheelMavenPluginTest {

  private Verifier verifier;
  private final List<String> analysisGoals = Arrays.asList("compile","com.github.fburato:highwheel-modules-maven-plugin:analyse");

  @Before
  public void setUp() throws Exception {
    verifier = new Verifier(Paths
        .get("target","test-classes","test-example")
        .toAbsolutePath()
        .toString()
    );
  }

  @Test
  public void shouldSucceedOnStrictBaseSpecification() throws Exception {
    verifier.executeGoals(analysisGoals);

    verifier.verifyErrorFreeLog();
    final String logFile = contentOf(Paths.get(verifier.getBasedir(),verifier.getLogFileName()).toFile());
    assertThat(logFile)
        .matches("(?s).*Facade --> fanIn:     2, fanOut:     3.*"          )
        .matches("(?s).*Utils --> fanIn:     2, fanOut:     0.*"           )
        .matches("(?s).*IO --> fanIn:     1, fanOut:     3.*"              )
        .matches("(?s).*Model --> fanIn:     4, fanOut:     0.*"           )
        .matches("(?s).*CoreInternals --> fanIn:     1, fanOut:     3.*"   )
        .matches("(?s).*CoreApi --> fanIn:     4, fanOut:     1.*"         )
        .matches("(?s).*Controller --> fanIn:     1, fanOut:     1.*"      )
        .matches("(?s).*Main --> fanIn:     0, fanOut:     4.*"            )
        .matches("(?s).*No dependency violation detected.*")
        .matches("(?s).*No direct dependency violation detected.*");
  }

  @Test
  public void shouldFailOnNonExistingSpec(){
    verifier.getCliOptions().add("-DhwmSpecFiles=not-a-spec");
    try {
      verifier.executeGoals(analysisGoals);
      fail("Exception expected");
    } catch(Exception e) {
      final String logFile = contentOf(Paths.get(verifier.getBasedir(),verifier.getLogFileName()).toFile());
      assertThat(logFile)
          .matches("(?s).*Cannot read from specification file 'not-a-spec'.*");
    }
  }

  @Test
  public void shouldFailOnWrongSpec() {
    verifier.getCliOptions().add("-DhwmSpecFiles=wrong-strict-spec.hwm");
    try {
      verifier.executeGoals(analysisGoals);
      fail("Exception expected");
    } catch(Exception e) {
      final String logFile = contentOf(Paths.get(verifier.getBasedir(),verifier.getLogFileName()).toFile());
      assertThat(logFile)
          .matches("(?s).*The following dependencies violate the specification:.*")
          .matches("(?s).*The following direct dependencies violate the specification:.*");
    }
  }
}
