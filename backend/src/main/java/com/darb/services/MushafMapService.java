package com.darb.services;

import com.darb.dtos.mushaf.HalfPageEntry;
import com.darb.dtos.mushaf.MushafMetadataResponse;
import com.darb.entities.enums.PageHalf;
import com.darb.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MushafMapService {

    private static final String EDITION = "MADINAH_604";
    private static final String MAP_RESOURCE = "mushaf/madinah-604.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MushafMetadataResponse metadata;
    private List<HalfPageEntry> allHalves = List.of();
    private Map<String, HalfPageEntry> halvesById = Map.of();
    private Map<Integer, List<HalfPageEntry>> halvesByPage = Map.of();
    private Map<Integer, List<HalfPageEntry>> halvesByJuz = Map.of();

    @PostConstruct
    public void loadMap() {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(new ClassPathResource(MAP_RESOURCE).getInputStream());
            List<HalfPageEntry> halves = new ArrayList<>();
            for (JsonNode node : root.get("halves")) {
                halves.add(parseHalf(node));
            }
            halves.sort(Comparator.comparing(HalfPageEntry::getPage)
                    .thenComparing(h -> "A".equals(h.getHalf()) ? 0 : 1));

            Map<String, HalfPageEntry> byId = new HashMap<>();
            Map<Integer, List<HalfPageEntry>> byPage = new HashMap<>();
            Map<Integer, List<HalfPageEntry>> byJuz = new HashMap<>();
            for (HalfPageEntry half : halves) {
                byId.put(half.getId(), half);
                byPage.computeIfAbsent(half.getPage(), k -> new ArrayList<>()).add(half);
                byJuz.computeIfAbsent(half.getJuz(), k -> new ArrayList<>()).add(half);
            }

            this.allHalves = List.copyOf(halves);
            this.metadata = MushafMetadataResponse.builder()
                    .edition(root.path("edition").asText(EDITION))
                    .pageCount(root.path("pageCount").asInt(604))
                    .juzCount(root.path("juzCount").asInt(30))
                    .halfCount(root.path("halfCount").asInt(halves.size()))
                    .juzStartPages(readIntList(root.get("juzStartPages")))
                    .halves(halves)
                    .build();
            this.halvesById = Map.copyOf(byId);
            this.halvesByPage = Map.copyOf(byPage);
            this.halvesByJuz = Map.copyOf(byJuz);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mushaf map: " + MAP_RESOURCE, e);
        }
    }

    public List<HalfPageEntry> getAllHalves() {
        return allHalves;
    }

    public HalfPageEntry getHalf(int page, String half) {
        return findHalfById(page + "-" + half)
                .orElseThrow(() -> new BadRequestException("memorization.invalidHalfPage"));
    }

    public HalfPageEntry requireHalf(int page, PageHalf half) {
        return getHalf(page, half.name());
    }

    public MushafMetadataResponse getMetadata() {
        return metadata;
    }

    public List<HalfPageEntry> getHalvesForJuz(int juz) {
        return List.copyOf(halvesByJuz.getOrDefault(juz, List.of()));
    }

    public List<HalfPageEntry> getHalvesForPage(int page) {
        return List.copyOf(halvesByPage.getOrDefault(page, List.of()));
    }

    public Optional<HalfPageEntry> findHalfById(String id) {
        return Optional.ofNullable(halvesById.get(id));
    }

    public void requireHalfPageIds(List<String> halfPageIds) {
        for (String id : halfPageIds) {
            if (!halvesById.containsKey(id)) {
                throw new BadRequestException("memorization.invalidHalfPage");
            }
        }
    }

    public boolean isAyahInHalf(int page, String half, int surah, int ayah) {
        return isAyahInHalf(getHalf(page, half), surah, ayah);
    }

    public boolean isAyahInHalf(HalfPageEntry half, int surah, int ayah) {
        return compareAyah(surah, ayah, half.getSurahFrom(), half.getAyahFrom()) >= 0
                && compareAyah(surah, ayah, half.getSurahTo(), half.getAyahTo()) <= 0;
    }

    public void validateAyahInHalf(HalfPageEntry half, int surah, int ayah) {
        if (!isAyahInHalf(half, surah, ayah)) {
            throw new BadRequestException("memorization.ayahOutOfRange");
        }
    }

    private static int compareAyah(int surahA, int ayahA, int surahB, int ayahB) {
        if (surahA != surahB) {
            return Integer.compare(surahA, surahB);
        }
        return Integer.compare(ayahA, ayahB);
    }

    private static HalfPageEntry parseHalf(JsonNode node) {
        return HalfPageEntry.builder()
                .id(node.path("id").asText())
                .edition(node.path("edition").asText(EDITION))
                .page(node.path("page").asInt())
                .half(node.path("half").asText())
                .juz(node.path("juz").asInt())
                .surahFrom(node.path("surahFrom").asInt())
                .ayahFrom(node.path("ayahFrom").asInt())
                .surahTo(node.path("surahTo").asInt())
                .ayahTo(node.path("ayahTo").asInt())
                .build();
    }

    private static List<Integer> readIntList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        node.forEach(n -> values.add(n.asInt()));
        return values;
    }

    private static final int[] SURAH_LENGTHS = {
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111,
            110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83,
            182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96,
            29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31,
            50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5,
            8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    };

    /** Global ayah index for contiguity checks; trusts mushaf map ayah numbers. */
    public static int ayahOrdinal(int surah, int ayah) {
        int ordinal = 0;
        for (int i = 0; i < surah - 1 && i < SURAH_LENGTHS.length; i++) {
            ordinal += SURAH_LENGTHS[i];
        }
        return ordinal + ayah;
    }
}
