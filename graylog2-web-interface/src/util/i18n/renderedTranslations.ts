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
import { singleton } from 'logic/singleton';

/**
 * Remembers which translation produced which text, so proofreaders can report a text they see on the page and the
 * report still points to the translation key. Nothing is added to the rendered texts themselves.
 */
export type TranslationSource = {
  namespace: string;
  key: string;
  /** Language the text was taken from. It differs from the current language when the translation is missing. */
  usedLanguage: string;
};

const MAX_ENTRIES = 5000;
const MAX_CANDIDATES = 5;
const MIN_PARTIAL_MATCH_LENGTH = 3;

// Tags of `<Trans>` components, e.g. `<0>`, `</bold>` or `<br/>`, are not part of the rendered text.
const TAG = /<\/?[\w-]+\s*\/?>/g;

export const normalizeText = (text: string) => text.replace(TAG, '').replace(/\s+/g, ' ').trim();

const textsToSources = singleton('i18n.renderedTranslations', () => new Map<string, Array<TranslationSource>>());

const isSameSource = (a: TranslationSource, b: TranslationSource) => a.namespace === b.namespace && a.key === b.key;

export const recordTranslation = (text: unknown, source: TranslationSource) => {
  if (typeof text !== 'string') {
    return;
  }

  const normalized = normalizeText(text);

  if (!normalized) {
    return;
  }

  const existing = textsToSources.get(normalized) ?? [];
  const sources = [...existing.filter((other) => !isSameSource(other, source)), source];

  // Re-inserting keeps the most recently rendered texts at the end, so the oldest ones are dropped first.
  textsToSources.delete(normalized);
  textsToSources.set(normalized, sources);

  if (textsToSources.size > MAX_ENTRIES) {
    textsToSources.delete(textsToSources.keys().next().value);
  }
};

export type Candidate = TranslationSource & { text: string; exact: boolean };

/**
 * Returns the translations which most likely produced the given text. Texts which were rendered exactly like this
 * come first. Otherwise, texts containing or contained in it are returned, closest in length first,
 * e.g. for an element combining a translation with a value.
 */
export const findTranslations = (text: string): Array<Candidate> => {
  const normalized = normalizeText(text);

  if (!normalized) {
    return [];
  }

  const exact = textsToSources.get(normalized);

  if (exact) {
    return exact.map((source) => ({ ...source, text: normalized, exact: true }));
  }

  const partial: Array<Candidate & { distance: number }> = [];

  textsToSources.forEach((sources, recorded) => {
    if (recorded.length < MIN_PARTIAL_MATCH_LENGTH) {
      return;
    }

    if (normalized.includes(recorded) || recorded.includes(normalized)) {
      sources.forEach((source) =>
        partial.push({
          ...source,
          text: recorded,
          exact: false,
          distance: Math.abs(recorded.length - normalized.length),
        }),
      );
    }
  });

  return partial
    .sort((a, b) => a.distance - b.distance)
    .filter((candidate, index, all) => all.findIndex((other) => isSameSource(other, candidate)) === index)
    .slice(0, MAX_CANDIDATES)
    .map(({ distance: _distance, ...candidate }) => candidate);
};

export const clearRenderedTranslations = () => textsToSources.clear();
