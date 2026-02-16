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
import { useState, useCallback } from 'react';

export type OnSelect<T> = (item: T, index: number) => void;

const PAGE_SIZE = 10;

const useListKeyboardNavigation = <T>(
  items: T[],
  onSelect?: OnSelect<T>,
): {
  highlightedIndex: number;
  setHighlightedIndex: React.Dispatch<React.SetStateAction<number>>;
  onKeyDown: (e: React.KeyboardEvent) => void;
} => {
  const [highlightedIndex, setHighlightedIndex] = useState<number>(-1);

  const onKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (!items || items.length === 0) return;

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setHighlightedIndex((prev) => (prev + 1) % items.length);
          break;
        case 'ArrowUp':
          e.preventDefault();
          setHighlightedIndex((prev) => (prev - 1 + items.length) % items.length);
          break;
        case 'PageDown':
          e.preventDefault();
          setHighlightedIndex((prev) => Math.min(prev + PAGE_SIZE, items.length - 1));
          break;
        case 'PageUp':
          e.preventDefault();
          setHighlightedIndex((prev) => Math.max(prev - PAGE_SIZE, 0));
          break;
        case 'Home':
          e.preventDefault();
          setHighlightedIndex(0);
          break;
        case 'End':
          e.preventDefault();
          setHighlightedIndex(items.length - 1);
          break;
        case 'Enter':
          e.preventDefault();
          if (highlightedIndex >= 0 && onSelect) {
            onSelect(items[highlightedIndex], highlightedIndex);
          }
          break;
        default:
          break;
      }
    },
    [highlightedIndex, items, onSelect],
  );

  return { highlightedIndex, setHighlightedIndex, onKeyDown };
};

export default useListKeyboardNavigation;
