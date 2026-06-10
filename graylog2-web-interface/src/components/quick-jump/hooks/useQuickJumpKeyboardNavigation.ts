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
import type React from 'react';
import { useCallback, useEffect, useMemo, useRef } from 'react';

import type Input from 'components/bootstrap/Input';
import useHistory from 'routing/useHistory';
import useListKeyboardNavigation from 'hooks/useListKeyboardNavigation';

import useActionArguments from './useActionArguments';

import type { SearchResultItem } from '../Types';

type Options = {
  items: SearchResultItem[];
  onToggle: () => void;
  searchQuery: string;
};

export type QuickJumpItemProps = {
  tabIndex: number;
  ref: (node: HTMLSpanElement | null) => void;
  onMouseEnter: () => void;
  onFocus: () => void;
  onClick: () => void;
};

type Result = {
  highlightedIndex: number;
  modalProps: {
    onKeyDownCapture: (event: React.KeyboardEvent) => void;
  };
  searchInputProps: {
    ref: React.MutableRefObject<React.ComponentRef<typeof Input> | null>;
  };
  getItemProps: (index: number) => QuickJumpItemProps;
  onHide: () => void;
};

const useQuickJumpKeyboardNavigation = ({ items, onToggle, searchQuery }: Options): Result => {
  const actionArguments = useActionArguments();
  const history = useHistory();
  const searchInputRef = useRef<React.ComponentRef<typeof Input>>(null);
  const itemRefs = useRef<(HTMLSpanElement | null)[]>([]);
  const isClosingRef = useRef(false);
  const skipInitialHoverRef = useRef(false);
  const previousQueryRef = useRef<string>();

  const focusSearchInput = useCallback(() => {
    const node = searchInputRef.current?.getInputDOMNode?.();

    if (node && document.activeElement !== node) {
      node.focus();
    }
  }, []);

  const onHide = useCallback(() => {
    isClosingRef.current = true;
    onToggle();
  }, [onToggle]);

  const handleItemSelect = useCallback(
    (item: SearchResultItem) => {
      if ('link' in item) {
        history.push(item.link);
      } else if ('externalLink' in item) {
        window.open(item.externalLink, '_blank', 'noopener');
      } else {
        item.action(actionArguments);
      }

      onHide();
    },
    [actionArguments, history, onHide],
  );

  const { highlightedIndex, setHighlightedIndex, onKeyDown } = useListKeyboardNavigation<SearchResultItem>(
    items,
    handleItemSelect,
  );

  useEffect(() => {
    isClosingRef.current = false;
    focusSearchInput();
  }, [focusSearchInput]);

  useEffect(() => {
    if (highlightedIndex < 0) {
      return;
    }

    itemRefs.current[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
  }, [highlightedIndex]);

  useEffect(() => {
    const queryChanged = previousQueryRef.current !== searchQuery;
    const listIsEmpty = items.length === 0;

    if (listIsEmpty) {
      setHighlightedIndex(-1);
      skipInitialHoverRef.current = false;
    } else if (queryChanged || highlightedIndex === -1) {
      skipInitialHoverRef.current = false;
      setHighlightedIndex(0);
    }

    previousQueryRef.current = searchQuery;
  }, [highlightedIndex, items, searchQuery, setHighlightedIndex]);

  const getItemProps = useCallback(
    (index: number): QuickJumpItemProps => ({
      tabIndex: -1,
      ref: (node: HTMLSpanElement | null) => {
        itemRefs.current[index] = node;
      },
      onMouseEnter: () => {
        if (!skipInitialHoverRef.current) {
          skipInitialHoverRef.current = true;
        } else {
          setHighlightedIndex(index);
        }
      },
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
    [handleItemSelect, items, setHighlightedIndex],
  );

  const searchInputProps = useMemo(
    () => ({
      ref: searchInputRef,
    }),
    [],
  );

  const handleKeyDownCapture = useCallback(
    (event: React.KeyboardEvent) => {
      onKeyDown(event);

      if (isClosingRef.current) {
        return;
      }

      const input = searchInputRef.current?.getInputDOMNode?.();

      if (!input || event.target === input) {
        return;
      }

      if (event.metaKey || event.altKey || event.ctrlKey) {
        return;
      }

      const { key } = event;
      const isCharacter = key.length === 1;
      const isEditingKey = key === 'Backspace' || key === 'Delete';

      if (!isCharacter && !isEditingKey) {
        return;
      }

      focusSearchInput();
    },
    [focusSearchInput, onKeyDown],
  );

  const modalProps = useMemo(
    () => ({
      onKeyDownCapture: handleKeyDownCapture,
    }),
    [handleKeyDownCapture],
  );

  return useMemo(
    () => ({
      highlightedIndex,
      searchInputProps,
      modalProps,
      getItemProps,
      onHide,
    }),
    [highlightedIndex, modalProps, searchInputProps, getItemProps, onHide],
  );
};

export default useQuickJumpKeyboardNavigation;
