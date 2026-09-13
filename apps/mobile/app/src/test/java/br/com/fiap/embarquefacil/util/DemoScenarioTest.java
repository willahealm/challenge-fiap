package br.com.fiap.embarquefacil.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DemoScenarioTest {
    @Test public void platformChangesOnFourthJourneySnapshot() {
        assertFalse(DemoScenario.shouldPublishPlatformChange(3));
        assertTrue(DemoScenario.shouldPublishPlatformChange(4));
    }

    @Test public void acceptsTheSameCheckpointAsTheLocalApi() {
        assertTrue(DemoScenario.isValidCheckpoint("tiete-totem-01"));
        assertFalse(DemoScenario.isValidCheckpoint("RIO-ENTRADA"));
    }
}
