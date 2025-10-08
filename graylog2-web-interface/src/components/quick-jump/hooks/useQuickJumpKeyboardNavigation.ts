/*
 * Copyright (C) 2024 Graylog, Inc.
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
import { useCallback, useEffect, useMemo, useRef } from 'react';
import type React from 'react';

import type Input from 'components/bootstrap/Input';
import useHistory from 'routing/useHistory';
import useListKeyboardNavigation from 'hooks/useListKeyboardNavigation';

import type { SearchResultItem } from '../Types';

type Options = {
  items: SearchResultItem[];
  onToggle: () => void;
};

export type QuickJumpItemProps = {
  ref: (node: HTMLSpanElement | null) => void;
  onMouseEnter: () => void;
  onMouseDown: (event: React.MouseEvent) => void;
  onFocus: () => void;
  onClick: () => void;
};

type Result = {
  highlightedIndex: number;
  searchInputProps: {
    ref: React.MutableRefObject<React.ComponentRef<typeof Input> | null>;
    onKeyDown: (event: React.KeyboardEvent) => void;
    onBlur: () => void;
  };
  getItemProps: (index: number) => QuickJumpItemProps;
  onHide: () => void;
};

const useQuickJumpKeyboardNavigation = ({ items, onToggle }: Options): Result => {
  const history = useHistory();
  const searchInputRef = useRef<React.ComponentRef<typeof Input>>(null);
  const itemRefs = useRef<(HTMLSpanElement | null)[]>([]);
  const isClosingRef = useRef(false);
  const blurTimeoutRef = useRef<number | null>(null);

  const focusSearchInput = useCallback(() => {
    const node = searchInputRef.current?.getInputDOMNode?.();

    if (node && document.activeElement !== node) {
      node.focus();
    }
  }, []);

  const clearBlurTimeout = useCallback(() => {
    if (blurTimeoutRef.current) {
      window.clearTimeout(blurTimeoutRef.current);
      blurTimeoutRef.current = null;
    }
  }, []);

  const onHide = useCallback(() => {
    isClosingRef.current = true;
    clearBlurTimeout();
    onToggle();
  }, [clearBlurTimeout, onToggle]);

  const handleItemSelect = useCallback(
    (item: SearchResultItem) => {
      if ((item as any)?.link) {
        history.push((item as any)?.link);
      }

      onHide();
    },
    [history, onHide],
  );

  const { highlightedIndex, setHighlightedIndex, onKeyDown } =
    useListKeyboardNavigation<SearchResultItem>(items, handleItemSelect);

  useEffect(() => {
    isClosingRef.current = false;
    focusSearchInput();

    return () => {
      clearBlurTimeout();
    };
  }, [clearBlurTimeout, focusSearchInput]);

  useEffect(() => {
    itemRefs.current.length = items.length;
  }, [items.length]);

  useEffect(() => {
    if (highlightedIndex < 0) {
      return;
    }

    itemRefs.current[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
  }, [highlightedIndex]);

  const handleSearchInputKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (items.length === 0) {
        return;
      }

      if (event.key === 'Enter') {
        event.preventDefault();

        const index = highlightedIndex === -1 ? 0 : highlightedIndex;
        const item = items[index];

        if (item) {
          if (highlightedIndex === -1) {
            setHighlightedIndex(index);
          }

          handleItemSelect(item);
        }

        return;
      }

      if (event.key === 'ArrowDown') {
        event.preventDefault();

        if (highlightedIndex === -1) {
          setHighlightedIndex(0);

          return;
        }
      }

      if (event.key === 'ArrowUp') {
        if (highlightedIndex === -1) {
          return;
        }

        if (highlightedIndex === 0) {
          event.preventDefault();
          setHighlightedIndex(-1);

          return;
        }
      }

      onKeyDown(event);
    },
    [handleItemSelect, highlightedIndex, items, onKeyDown, setHighlightedIndex],
  );

  const didInitialHighlightRef = useRef(false);

  useEffect(() => {
    if (items.length === 0) {
      setHighlightedIndex(-1);

      return;
    }

    setHighlightedIndex((prev) => {
      if (prev !== -1) {
        return prev;
      }

      if (didInitialHighlightRef.current) {
        return prev;
      }

      didInitialHighlightRef.current = true;

      return 0;
    });
  }, [items, setHighlightedIndex]);

  const handleSearchInputBlur = useCallback(() => {
    if (isClosingRef.current) {
      return;
    }

    blurTimeoutRef.current = window.setTimeout(() => {
      if (!isClosingRef.current) {
        focusSearchInput();
      }

      blurTimeoutRef.current = null;
    }, 0);
  }, [focusSearchInput]);

  const handleItemMouseDown = useCallback(
    (event: React.MouseEvent) => {
      if (event.defaultPrevented) {
        return;
      }

      event.preventDefault();
      focusSearchInput();
    },
    [focusSearchInput],
  );

  const getItemProps = useCallback(
    (index: number): QuickJumpItemProps => ({
      ref: (node: HTMLSpanElement | null) => {
        itemRefs.current[index] = node;
      },
      onMouseEnter: () => {
        setHighlightedIndex(index);
      },
      onMouseDown: handleItemMouseDown,
      onFocus: () => {
        setHighlightedIndex(index);
      },
      onClick: () => {
        setHighlightedIndex(index);

        const item = items[index];

        if (item) {
          handleItemSelect(item);
        }
      },
    }),
    [handleItemMouseDown, handleItemSelect, items, setHighlightedIndex],
  );

  const searchInputProps = useMemo(
    () => ({
      ref: searchInputRef,
      onKeyDown: handleSearchInputKeyDown,
      onBlur: handleSearchInputBlur,
    }),
    [handleSearchInputBlur, handleSearchInputKeyDown],
  );

  return useMemo(
    () => ({
      highlightedIndex,
      searchInputProps,
      getItemProps,
      onHide,
    }),
    [
      searchInputProps,
      getItemProps,
      onHide,
      highlightedIndex,
    ],
  );
};

export default useQuickJumpKeyboardNavigation;
