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
import { useSyncExternalStore } from 'react';

import Store from 'logic/local-storage/Store';

export type SidebarBounds = { initial: number; min: number; max: number };

export const clampWidth = (width: number, { min, max }: SidebarBounds) =>
  Math.min(max, Math.max(min, Math.round(width)));

// Used while local storage is unavailable, e.g. in private windows, so a width lasts until the page is reloaded.
const unstoredWidths = new Map<string, number>();
const listeners = new Map<string, Set<() => void>>();

const storedWidth = (storageKey: string) => {
  try {
    const width = Store.get<number>(storageKey);

    return typeof width === 'number' && Number.isFinite(width) ? width : undefined;
  } catch {
    return unstoredWidths.get(storageKey);
  }
};

const storeWidth = (storageKey: string, width: number) => {
  try {
    Store.set(storageKey, width);
  } catch {
    unstoredWidths.set(storageKey, width);
  }

  listeners.get(storageKey)?.forEach((listener) => listener());
};

const subscribe = (storageKey: string) => (listener: () => void) => {
  const keyListeners = listeners.get(storageKey) ?? new Set<() => void>();
  keyListeners.add(listener);
  listeners.set(storageKey, keyListeners);

  return () => {
    keyListeners.delete(listener);
  };
};

/**
 * The width of a resizable sidebar, kept in the browser's local storage under `storageKey`. Sidebars using the same
 * key share their width.
 */
const useSidebarWidth = (storageKey: string, bounds: SidebarBounds) => {
  const width = useSyncExternalStore(subscribe(storageKey), () => storedWidth(storageKey));
  const setWidth = (next: number) => storeWidth(storageKey, clampWidth(next, bounds));

  return [clampWidth(width ?? bounds.initial, bounds), setWidth] as const;
};

export default useSidebarWidth;
