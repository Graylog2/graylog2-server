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

type Entry = {
  depth: number;
  title: string;
};

const entries = singleton('components.common.documentTitleEntries', () => new Map<symbol, Entry>());

// Deepest entry wins, ties go to the most recently registered one (e.g. the incoming page during a route change).
const currentTitle = (fallback: string) => {
  let top: Entry | undefined;

  entries.forEach((entry) => {
    if (!top || entry.depth >= top.depth) {
      top = entry;
    }
  });

  return top?.title ?? fallback;
};

export const registerDocumentTitle = (id: symbol, entry: Entry, productName: string) => {
  entries.set(id, entry);
  document.title = currentTitle(productName);
};

export const unregisterDocumentTitle = (id: symbol, productName: string) => {
  entries.delete(id);
  document.title = currentTitle(productName);
};
