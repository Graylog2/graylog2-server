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

export const clampSize = (size: number, { min, max }: SidebarBounds) => Math.min(max, Math.max(min, Math.round(size)));

// Used while local storage is unavailable, e.g. in private windows, so a size lasts until the page is reloaded.
const unstoredSizes = new Map<string, number>();
const listeners = new Map<string, Set<() => void>>();

const storedSize = (storageKey: string) => {
  try {
    const size = Store.get<number>(storageKey);

    return typeof size === 'number' && Number.isFinite(size) ? size : undefined;
  } catch {
    return unstoredSizes.get(storageKey);
  }
};

const storeSize = (storageKey: string, size: number) => {
  try {
    Store.set(storageKey, size);
  } catch {
    unstoredSizes.set(storageKey, size);
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
 * The size of a resizable sidebar, its width or height, kept in the browser's local storage under `storageKey`.
 * Sidebars using the same key share their size.
 */
const useSidebarSize = (storageKey: string, bounds: SidebarBounds) => {
  const size = useSyncExternalStore(subscribe(storageKey), () => storedSize(storageKey));
  const setSize = (next: number) => storeSize(storageKey, clampSize(next, bounds));

  return [clampSize(size ?? bounds.initial, bounds), setSize] as const;
};

export default useSidebarSize;
