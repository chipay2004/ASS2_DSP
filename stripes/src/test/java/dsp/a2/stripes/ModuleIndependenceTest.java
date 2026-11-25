package dsp.a2.stripes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the agreed module structure: Stripes must build and run WITHOUT the Pairs module.
 * If someone adds a dependency on Pairs (directly or through common), this test fails.
 */
class ModuleIndependenceTest {

    @Test
    void pairsClassesAreNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("dsp.a2.pairs.PairsDriver"));
    }
}
