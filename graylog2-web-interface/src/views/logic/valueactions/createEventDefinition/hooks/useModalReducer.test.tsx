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
import React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { act } from '@testing-library/react-hooks';

import {
  modalDataResult,
} from 'fixtures/createEventDefinitionFromValue';
import useModalReducer from 'views/logic/valueactions/createEventDefinition/hooks/useModalReducer';

const wrapper = ({ children }) => (
  <div>
    {children}
  </div>
);

const defaultState = {
  strategy: 'EXACT',
  showDetails: false,
  checked: {
    aggCondition: true,
    columnGroupBy: true,
    lutParameters: true,
    queryWithReplacedParams: true,
    rowGroupBy: true,
    searchFilterQuery: true,
    searchFromValue: true,
    searchWithinMs: true,
    streams: true,
  },
};

describe('useModalReducer', () => {
  it('show default state', async () => {
    const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });

    const [store] = result.current;

    await expect(store).toEqual(defaultState);
  });

  describe('switching strategy shows correct state for', () => {
    it('custom', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_CUSTOM_STRATEGY' });
      });

      const store = result.current[0];

      return expect(store).toEqual({
        strategy: 'CUSTOM',
        showDetails: true,
        checked: {
          aggCondition: true,
          columnGroupBy: true,
          lutParameters: true,
          queryWithReplacedParams: true,
          rowGroupBy: true,
          searchFilterQuery: true,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });

    it('column', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_COL_STRATEGY' });
      });

      const store = result.current[0];

      expect(store).toEqual({
        strategy: 'COL',
        showDetails: false,
        checked: {
          aggCondition: true,
          columnGroupBy: true,
          lutParameters: true,
          queryWithReplacedParams: true,
          rowGroupBy: false,
          searchFilterQuery: true,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });

    it('show', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_ROW_STRATEGY' });
      });

      const store = result.current[0];

      expect(store).toEqual({
        strategy: 'ROW',
        showDetails: false,
        checked: {
          aggCondition: true,
          columnGroupBy: false,
          lutParameters: true,
          queryWithReplacedParams: true,
          rowGroupBy: true,
          searchFilterQuery: true,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });

    it('widget', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_ALL_STRATEGY' });
      });

      const store = result.current[0];

      expect(store).toEqual({
        strategy: 'ALL',
        showDetails: false,
        checked: {
          aggCondition: true,
          columnGroupBy: true,
          lutParameters: true,
          queryWithReplacedParams: false,
          rowGroupBy: true,
          searchFilterQuery: false,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });

    it('exact', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_ALL_STRATEGY' });
      });

      act(() => {
        dispatch({ type: 'SET_EXACT_STRATEGY' });
      });

      const store = result.current[0];

      expect(store).toEqual({
        strategy: 'EXACT',
        showDetails: false,
        checked: {
          aggCondition: true,
          columnGroupBy: true,
          lutParameters: true,
          queryWithReplacedParams: true,
          rowGroupBy: true,
          searchFilterQuery: true,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });

    it('custom and keep prev selected state', async () => {
      const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
      const dispatch = result.current[1];

      act(() => {
        dispatch({ type: 'SET_ALL_STRATEGY' });
      });

      act(() => {
        dispatch({ type: 'SET_CUSTOM_STRATEGY' });
      });

      const store = result.current[0];

      return expect(store).toEqual({
        strategy: 'CUSTOM',
        showDetails: true,
        checked: {
          aggCondition: true,
          columnGroupBy: true,
          lutParameters: true,
          queryWithReplacedParams: false,
          rowGroupBy: true,
          searchFilterQuery: false,
          searchFromValue: true,
          searchWithinMs: true,
          streams: true,
        },
      });
    });
  });

  it('open and close details', async () => {
    const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
    const dispatch = result.current[1];

    act(() => {
      dispatch({ type: 'TOGGLE_SHOW_DETAILS' });
    });

    const store = result.current[0];

    return expect(store).toEqual({
      strategy: 'EXACT',
      showDetails: true,
      checked: {
        aggCondition: true,
        columnGroupBy: true,
        lutParameters: true,
        queryWithReplacedParams: true,
        rowGroupBy: true,
        searchFilterQuery: true,
        searchFromValue: true,
        searchWithinMs: true,
        streams: true,
      },
    });
  });

  it('update selected items and switch to custom strategy', async () => {
    const { result } = renderHook(() => useModalReducer(modalDataResult), { wrapper });
    const dispatch = result.current[1];

    act(() => {
      dispatch({
        type: 'UPDATE_CHECKED_ITEMS',
        payload: {
          queryWithReplacedParams: false,
          rowGroupBy: false,
          searchFilterQuery: false,
        },
      });
    });

    const store = result.current[0];

    return expect(store).toEqual({
      strategy: 'CUSTOM',
      showDetails: false,
      checked: {
        aggCondition: true,
        columnGroupBy: true,
        lutParameters: true,
        queryWithReplacedParams: false,
        rowGroupBy: false,
        searchFilterQuery: false,
        searchFromValue: true,
        searchWithinMs: true,
        streams: true,
      },
    });
  });
});
