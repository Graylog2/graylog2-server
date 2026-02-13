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
import React, { useReducer, useCallback, useMemo } from 'react';

import RightSidebarContext from 'contexts/RightSidebarContext';
import type { RightSidebarContent } from 'contexts/RightSidebarContext';

type Props = {
  children: React.ReactNode;
};

const MAX_HISTORY_DEPTH = 20;

type HistoryState = {
  contentHistory: Array<RightSidebarContent>;
  currentIndex: number;
  isOpen: boolean;
  isCollapsed: boolean;
  width: number;
};

type HistoryAction =
  | { type: 'OPEN_SIDEBAR'; content: RightSidebarContent }
  | { type: 'CLOSE_SIDEBAR' }
  | { type: 'COLLAPSE_SIDEBAR' }
  | { type: 'EXPAND_SIDEBAR' }
  | { type: 'UPDATE_CONTENT'; content: RightSidebarContent }
  | { type: 'GO_BACK' }
  | { type: 'GO_FORWARD' }
  | { type: 'SET_WIDTH'; width: number };

const initialState: HistoryState = {
  contentHistory: [],
  currentIndex: -1,
  isOpen: false,
  isCollapsed: false,
  width: 400,
};

const isSameContent = (content1: RightSidebarContent, content2: RightSidebarContent): boolean => content1.id === content2.id;

const historyReducer = (state: HistoryState, action: HistoryAction): HistoryState => {
  switch (action.type) {
    case 'OPEN_SIDEBAR': {
      if (state.currentIndex >= 0 && isSameContent(state.contentHistory[state.currentIndex], action.content)) {
        const newHistory = [...state.contentHistory];
        newHistory[state.currentIndex] = action.content;

        return {
          ...state,
          contentHistory: newHistory,
          isOpen: true,
          isCollapsed: false,
        };
      }

      const newHistory = state.contentHistory.slice(0, state.currentIndex + 1);
      newHistory.push(action.content);

      if (newHistory.length > MAX_HISTORY_DEPTH) {
        newHistory.shift();

        return {
          ...state,
          contentHistory: newHistory,
          currentIndex: MAX_HISTORY_DEPTH - 1,
          isOpen: true,
          isCollapsed: false,
        };
      }

      return {
        ...state,
        contentHistory: newHistory,
        currentIndex: newHistory.length - 1,
        isOpen: true,
        isCollapsed: false,
      };
    }

    case 'CLOSE_SIDEBAR': {
      return {
        ...state,
        contentHistory: [],
        currentIndex: -1,
        isOpen: false,
        isCollapsed: false,
      };
    }

    case 'COLLAPSE_SIDEBAR': {
      return {
        ...state,
        isCollapsed: true,
      };
    }

    case 'EXPAND_SIDEBAR': {
      return {
        ...state,
        isCollapsed: false,
      };
    }

    case 'UPDATE_CONTENT': {
      if (state.currentIndex === -1) {
        return state;
      }

      const newHistory = [...state.contentHistory];
      newHistory[state.currentIndex] = action.content;

      return {
        ...state,
        contentHistory: newHistory,
      };
    }

    case 'GO_BACK': {
      if (state.currentIndex <= 0) {
        return state;
      }

      return {
        ...state,
        currentIndex: state.currentIndex - 1,
      };
    }

    case 'GO_FORWARD': {
      if (state.currentIndex >= state.contentHistory.length - 1) {
        return state;
      }

      return {
        ...state,
        currentIndex: state.currentIndex + 1,
      };
    }

    case 'SET_WIDTH': {
      return {
        ...state,
        width: action.width,
      };
    }

    default:
      return state;
  }
};

const RightSidebarProvider = ({ children }: Props) => {
  const [state, dispatch] = useReducer(historyReducer, initialState);

  const content = state.currentIndex >= 0 ? state.contentHistory[state.currentIndex] : null;
  const canGoBack = state.currentIndex > 0;
  const canGoForward = state.currentIndex < state.contentHistory.length - 1;

  const openSidebar = useCallback(
    <T = Record<string, unknown>,>(newContent: RightSidebarContent<T>) => {
      dispatch({ type: 'OPEN_SIDEBAR', content: newContent as RightSidebarContent<any> });
    },
    [],
  );

  const closeSidebar = useCallback(() => {
    dispatch({ type: 'CLOSE_SIDEBAR' });
  }, []);

  const collapseSidebar = useCallback(() => {
    dispatch({ type: 'COLLAPSE_SIDEBAR' });
  }, []);

  const expandSidebar = useCallback(() => {
    dispatch({ type: 'EXPAND_SIDEBAR' });
  }, []);

  const updateContent = useCallback(<T = Record<string, unknown>,>(newContent: RightSidebarContent<T>) => {
    dispatch({ type: 'UPDATE_CONTENT', content: newContent as RightSidebarContent<any> });
  }, []);

  const setWidth = useCallback((newWidth: number) => {
    dispatch({ type: 'SET_WIDTH', width: newWidth });
  }, []);

  const goBack = useCallback(() => {
    dispatch({ type: 'GO_BACK' });
  }, []);

  const goForward = useCallback(() => {
    dispatch({ type: 'GO_FORWARD' });
  }, []);

  const contextValue = useMemo(
    () => ({
      isOpen: state.isOpen,
      isCollapsed: state.isCollapsed,
      content,
      width: state.width,
      openSidebar,
      closeSidebar,
      collapseSidebar,
      expandSidebar,
      updateContent,
      setWidth,
      goBack,
      goForward,
      canGoBack,
      canGoForward,
    }),
    [state.isOpen, state.isCollapsed, content, state.width, openSidebar, closeSidebar, collapseSidebar, expandSidebar, updateContent, setWidth, goBack, goForward, canGoBack, canGoForward],
  );

  return <RightSidebarContext.Provider value={contextValue}>{children}</RightSidebarContext.Provider>;
};

export default RightSidebarProvider;
