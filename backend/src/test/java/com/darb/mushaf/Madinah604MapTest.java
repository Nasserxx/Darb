package com.darb.mushaf;

import com.darb.dtos.mushaf.HalfPageEntry;
import com.darb.services.MushafMapService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Madinah604MapTest {

    static MushafMapService mushafMapService;

    @BeforeAll
    static void setUp() {
        mushafMapService = new MushafMapService();
        mushafMapService.loadMap();
    }

    @Test
    void map_has1208Halves() {
        assertThat(mushafMapService.getAllHalves()).hasSize(1208);
    }

    @Test
    void juz1StartsPage1() {
        assertThat(mushafMapService.getHalf(1, "A").getJuz()).isEqualTo(1);
    }

    @Test
    void juz30StartsPage582() {
        assertTrue(mushafMapService.getHalvesForJuz(30).stream().anyMatch(h -> h.getPage() == 582));
    }

    @Test
    void eachPageHalvesAreContiguousOrShared() {
        for (int page = 1; page <= 604; page++) {
            HalfPageEntry halfA = mushafMapService.getHalf(page, "A");
            HalfPageEntry halfB = mushafMapService.getHalf(page, "B");
            assertThat(halfA.getPage()).isEqualTo(page);
            assertThat(halfB.getPage()).isEqualTo(page);
            assertThat(halfA.getJuz()).isEqualTo(halfB.getJuz());

            int aEnd = MushafMapService.ayahOrdinal(halfA.getSurahTo(), halfA.getAyahTo());
            int bStart = MushafMapService.ayahOrdinal(halfB.getSurahFrom(), halfB.getAyahFrom());
            assertThat(bStart).isIn(aEnd, aEnd + 1);
        }
    }

    @Test
    void halfIdsAreUnique() {
        List<HalfPageEntry> all = mushafMapService.getAllHalves();
        Set<String> ids = new HashSet<>();
        for (HalfPageEntry entry : all) {
            assertThat(ids.add(entry.getId())).isTrue();
            assertThat(entry.getId()).isEqualTo(entry.getPage() + "-" + entry.getHalf());
        }
    }

    @Test
    void juz30Contains604() {
        assertTrue(mushafMapService.getHalvesForJuz(30).stream().anyMatch(h -> h.getPage() == 604));
    }

    @Test
    void isAyahInHalf_respectsBoundaries() {
        HalfPageEntry half = mushafMapService.getHalf(1, "A");
        assertTrue(mushafMapService.isAyahInHalf(1, "A", half.getSurahFrom(), half.getAyahFrom()));
        assertTrue(mushafMapService.isAyahInHalf(1, "A", half.getSurahTo(), half.getAyahTo()));
    }
}
