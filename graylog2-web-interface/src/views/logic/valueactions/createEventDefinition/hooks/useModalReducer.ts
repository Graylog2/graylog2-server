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

import { useCallback, useEffect, useMemo, useReducer } from 'react';
import objectHas from 'lodash/has';
import mapValues from 'lodash/mapValues';

import type { Checked, ModalData, State } from 'views/logic/valueactions/createEventDefinition/types';

export const initState: State = {
  strategy: 'EXACT',
  checked: {},
  showDetails: false,
};

const updateIfHas = (possibleKeys: Checked, updates:Checked): Checked => {
  const newState = { ...possibleKeys };

  Object.entries(updates).forEach(([key, value]) => {
    if (objectHas(possibleKeys, key)) {
      newState[key] = value;
    }
  });

  return newState;
};

const reducer = (state: State, action: { type: string, payload?: Checked, possibleKeys: Checked}): State => {
  const { type, payload, possibleKeys } = action;

  switch (type) {
    case 'SET_ALL_STRATEGY':
      return ({
        strategy: 'ALL',
        showDetails: state.showDetails,
        checked: updateIfHas(possibleKeys, { searchFilterQuery: false, queryWithReplacedParams: false }),
      });
    case 'SET_EXACT_STRATEGY':
      return ({
        strategy: 'EXACT',
        showDetails: state.showDetails,
        checked: possibleKeys,
      });
    case 'SET_ROW_STRATEGY':
      return ({
        strategy: 'ROW',
        showDetails: state.showDetails,
        checked: updateIfHas(possibleKeys, { columnValuePath: false, columnGroupBy: false }),
      });
    case 'SET_COL_STRATEGY':
      return ({
        strategy: 'COL',
        showDetails: state.showDetails,
        checked: updateIfHas(possibleKeys, { rowValuePath: false, rowGroupBy: false }),
      });
    case 'SET_CUSTOM_STRATEGY':
      return ({
        strategy: 'CUSTOM',
        showDetails: true,
        checked: state.checked,
      });
    case 'UPDATE_CHECKED_ITEMS':
      return ({
        strategy: 'CUSTOM',
        showDetails: state.showDetails,
        checked: updateIfHas(state.checked, payload),
      });
    case 'TOGGLE_SHOW_DETAILS':
      return ({
        strategy: state.strategy,
        showDetails: !state.showDetails,
        checked: state.checked,
      });
    default:
      return state;
  }
};

const useModalReducer = (modalData : ModalData): [State, ({ type, payload }: { type: string, payload?: Checked }) => void] => {
  const [state, dispatch] = useReducer(reducer, initState);
  const dispatchWithData = useCallback(({ type, payload }: { type: string, payload?: Checked }) => {
    const possibleKeys = mapValues(modalData, (v) => !!v);
    dispatch({ type, payload, possibleKeys });
  }, [modalData]);

  useEffect(() => {
    dispatchWithData({ type: 'SET_EXACT_STRATEGY' });
  }, [dispatchWithData]);

  return useMemo(() => [state, dispatchWithData], [state, dispatchWithData]);
};

export default useModalReducer;
