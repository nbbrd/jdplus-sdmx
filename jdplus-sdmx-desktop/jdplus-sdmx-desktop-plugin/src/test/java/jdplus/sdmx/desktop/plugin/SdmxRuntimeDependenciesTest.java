package jdplus.sdmx.desktop.plugin;

import jdplus.main.desktop.design.GAV;
import nbbrd.design.MightBePromoted;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import standalone_sdmxdl.nbbrd.io.FileParser;

import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static org.assertj.core.api.Assertions.assertThat;

public class SdmxRuntimeDependenciesTest {

    @Test
    public void test() throws IOException {
        assertThat(getRuntimeDependencies(SdmxRuntimeDependenciesTest.class))
                .describedAs("Check runtime dependencies")
                .satisfies(SdmxRuntimeDependenciesTest::checkSdmx)
                .satisfies(SdmxRuntimeDependenciesTest::checkSdmxdl)
                .satisfies(SdmxRuntimeDependenciesTest::checkJavaDesktopUtil)
                .hasSize(4);
    }

    private static void checkSdmx(List<? extends GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.jdplus-sdmx")
                .has(sameVersion())
                .extracting(GAV::getArtifactId)
                .are(matchingPattern(compile("^jdplus-sdmx-base-\\w+$")))
                .hasSize(1);
    }

    private static void checkSdmxdl(List<? extends GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.sdmx-dl")
                .has(sameVersion())
                .extracting(GAV::getArtifactId)
                .containsExactlyInAnyOrder("sdmx-dl-api", "sdmx-dl-standalone");
    }

    private static void checkJavaDesktopUtil(List<? extends GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.java-desktop-util")
                .has(sameVersion())
                .extracting(GAV::getArtifactId)
                .are(matchingPattern(compile("^java-desktop-util-(favicon)$")))
                .hasSize(1);
    }

    @MightBePromoted
    private static ListAssert<? extends GAV> assertThatGroupId(List<? extends GAV> coordinates, String groupId) {
        return assertThat(coordinates)
                .describedAs("Check " + groupId)
                .filteredOn(GAV::getGroupId, groupId);
    }

    @MightBePromoted
    private static Condition<List<? extends GAV>> sameVersion() {
        return new Condition<>(GAV::haveSameVersion, "same version");
    }

    @MightBePromoted
    private static Condition<String> matchingPattern(Pattern regex) {
        return new Condition<>(regex.asMatchPredicate(), "matching pattern");
    }

    @MightBePromoted
    private static List<GAV> getRuntimeDependencies(Class<?> anchor) throws IOException {
        return FileParser.onParsingStream(Manifest::new)
                .andThen(GAV::parseNbmMavenClassPath)
                .parseResource(anchor, "/runtime-dependencies.mf");
    }
}
