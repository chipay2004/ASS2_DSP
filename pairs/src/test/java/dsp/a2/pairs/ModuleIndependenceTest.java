package dsp.a2.pairs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the agreed module structure: Pairs must build and run WITHOUT the Stripes module.
 * If someone adds a dependency on Stripes (directly or through common), this test fails.
 */
class ModuleIndependenceTest {

    @Test
    void stripesClassesAreNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("dsp.a2.stripes.StripesDriver"));
    }
}
