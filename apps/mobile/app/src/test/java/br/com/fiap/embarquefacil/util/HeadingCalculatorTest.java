package br.com.fiap.embarquefacil.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HeadingCalculatorTest {
    @Test public void rotatesClockwiseAcrossNorth() {
        assertEquals(20f, HeadingCalculator.arrowRotation(10f, 350f), 0.001f);
    }

    @Test public void choosesShortestCounterClockwisePath() {
        assertEquals(-20f, HeadingCalculator.arrowRotation(350f, 10f), 0.001f);
    }

    @Test public void alignedHeadingIsZero() {
        assertEquals(0f, HeadingCalculator.arrowRotation(68f, 68f), 0.001f);
    }
}
