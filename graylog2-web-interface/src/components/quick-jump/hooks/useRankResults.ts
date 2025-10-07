/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import type { SearchResultItem } from 'components/quick-jump/Types';
import { PAGE_TYPE } from 'components/quick-jump/Constants';

export type ScoreContext = {
  query: string;
  categoryWeights?: { page: number; entity: number }; // default {1,1}
  minRelevance?: number; // default 0.35 on local score
};

// ---------- text similarity (fast + decent) ----------
const norm = (s: string) => s.toLowerCase().trim();
const tokenize = (s: string) => norm(s).split(/\s+/).filter(Boolean);

const exact = (q: string, t: string) => norm(t) === norm(q);
const startsWith = (q: string, t: string) => norm(t).startsWith(norm(q));
const acronym = (q: string, t: string) => {
  const ac = t
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .toLowerCase();

  return ac.startsWith(norm(q));
};
const subsequence = (q: string, t: string) => {
  let i = 0;
  const nq = norm(q),
    nt = norm(t);
  for (const c of nt) {
    if (c === nq[i]) {
      i = i + 1;
    }
  }

  return i === nq.length;
};
const jaccard = (q: string, t: string) => {
  const A = new Set(tokenize(q));
  const B = new Set(tokenize(t));
  const inter = [...A].filter((x) => B.has(x)).length;
  const union = new Set([...A, ...B]).size || 1;

  return inter / union; // 0..1
};
const bigramOverlap = (q: string, t: string) => {
  const grams = (s: string) => {
    const n = norm(s);
    const g = new Set<string>();
    for (let i = 0; i < n.length - 1; i = i + 1) {
      g.add(n.slice(i, i + 2));
    }

    return g;
  };
  const A = grams(q),
    B = grams(t);
  const inter = [...A].filter((x) => B.has(x)).length;
  const union = new Set([...A, ...B]).size || 1;

  return inter / union; // 0..1
};

const textScore = (query: string, title: string) => {
  if (!query) return 0.0;
  if (exact(query, title)) return 1.0;
  if (startsWith(query, title)) return 0.9;
  if (subsequence(query, title)) return 0.75;
  if (acronym(query, title)) return 0.7;

  const j = jaccard(query, title);
  const f = bigramOverlap(query, title);
  const combo = 0.6 * j + 0.4 * f;

  return Math.max(0.25, Math.min(0.8, combo)); // keep floors/ceiling sane
};

// ---------- local scorers ----------
const pageLocalScore = (item: SearchResultItem, ctx: ScoreContext) => textScore(ctx.query, item.title); // simple & predictable

const entityLocalScore = (item: SearchResultItem, ctx: ScoreContext) =>
  // Prefer backendScore if provided, otherwise fallback to title match
  typeof item.backendScore === 'number'
    ? Math.max(0, Math.min(1, item.backendScore))
    : textScore(ctx.query, item.title);

const useRankResults = (items: Array<SearchResultItem>, ctx: ScoreContext) => {
  const weights = { page: 1.0, entity: 1.0, ...(ctx.categoryWeights ?? {}) };
  const minRel = ctx.minRelevance ?? 0.35;

  if (!ctx.query.trim()) {
    return items;
  }

  // score, filter weak, dedupe by (title, link)
  const scored = items
    .map((item) => {
      const local = item.type === PAGE_TYPE ? pageLocalScore(item, ctx) : entityLocalScore(item, ctx);

      if (local < minRel) return null;

      const w = item.type === PAGE_TYPE ? weights.page : weights.entity;
      const final = Math.max(0, Math.min(1, local * w));

      return { item, local, final };
    })
    .filter(Boolean);

  // de-duplicate (prefer higher score)
  const key = (x: SearchResultItem) => `${norm(x.title)}|${x.link}`;
  const bestByKey = new Map<string, { item: SearchResultItem; final: number }>();
  for (const s of scored) {
    const k = key(s.item);
    const prev = bestByKey.get(k);
    if (!prev || s.final > prev.final) {
      bestByKey.set(k, { item: s.item, final: s.final });
    }
  }

  // sort by final desc, then title A–Z
  return [...bestByKey.values()]
    .sort((a, b) => b.final - a.final || a.item.title.localeCompare(b.item.title))
    .map((x) => x.item);
};

export default useRankResults;
