package lk.booknplay.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SportCatalogTest {

    @Test
    void cricketAliasesMergeToSingleCricket() {
        assertEquals("Cricket", SportCatalog.canonicalName("Indoor Cricket"));
        assertEquals("Cricket", SportCatalog.canonicalName("cricket"));
    }

    @Test
    void footballAndFutsalStayDistinct() {
        assertEquals("Football", SportCatalog.canonicalName("Football"));
        assertEquals("Football", SportCatalog.canonicalName("Soccer"));
        assertEquals("Football", SportCatalog.canonicalName("Football Field"));
        assertEquals("Futsal", SportCatalog.canonicalName("Futsal"));
        assertEquals("Futsal", SportCatalog.canonicalName("Indoor Football"));
        assertNotEquals(SportCatalog.canonicalName("Football"), SportCatalog.canonicalName("Futsal"));
    }

    @Test
    void tableTennisIsCheckedBeforeTennis() {
        assertEquals("Table Tennis", SportCatalog.canonicalName("Table Tennis"));
        assertEquals("Tennis", SportCatalog.canonicalName("Tennis Court"));
        assertEquals("Padel", SportCatalog.canonicalName("Padel"));
    }

    @Test
    void customSportsArePreservedTrimmed() {
        assertEquals("Carrom", SportCatalog.canonicalName("Carrom"));
    }

    @Test
    void resourceLabelsMapToPhysicalUnits() {
        assertEquals("Pitch", SportCatalog.resourceLabel("Futsal"));
        assertEquals("Pitch", SportCatalog.resourceLabel("Football"));
        assertEquals("Table", SportCatalog.resourceLabel("Table Tennis"));
        assertEquals("Pool Table", SportCatalog.resourceLabel("8-Ball Pool"));
        assertEquals("Lane", SportCatalog.resourceLabel("Swimming"));
        assertEquals("Court", SportCatalog.resourceLabel("Cricket"));
    }
}
