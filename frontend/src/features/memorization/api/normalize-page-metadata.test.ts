import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { normalizePageMetadata } from "./normalize-page-metadata.ts";

describe("normalizePageMetadata", () => {
  it("wraps a JSON array as { halves }", () => {
    const halves = [
      { id: "p1-a", page: 1, half: "A" },
      { id: "p1-b", page: 1, half: "B" },
    ];
    const result = normalizePageMetadata(halves);
    assert.deepEqual(result.halves, halves);
    assert.ok(Array.isArray(result.halves));
  });

  it("passthroughs an object that already has halves", () => {
    const meta = {
      page: 2,
      juz: 1,
      halves: [{ id: "p2-a", page: 2, half: "A" }],
    };
    assert.equal(normalizePageMetadata(meta), meta);
    assert.deepEqual(normalizePageMetadata(meta).halves, meta.halves);
  });
});
