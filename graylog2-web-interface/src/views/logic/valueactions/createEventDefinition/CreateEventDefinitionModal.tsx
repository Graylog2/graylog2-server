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
import React, { useMemo, useState, useReducer, useCallback, useEffect } from 'react';
import mapValues from 'lodash/mapValues';
import pick from 'lodash/pick';
import omit from 'lodash/omit';

import { Checkbox, Modal, Button } from 'components/bootstrap';
import type {
  ItemKey,
  Checked,
  State,
  ModalData,
} from 'views/logic/valueactions/createEventDefinition/types';
import CheckBoxGroup from 'views/logic/valueactions/createEventDefinition/CheckBoxGroup';
import {
  aggregationGroup,
  searchGroup,
  labels,
} from 'views/logic/valueactions/createEventDefinition/Constants';
import RadioSection from 'views/logic/valueactions/createEventDefinition/RadioSection';

const initState: State = {
  strategy: 'EXACT',
  checked: {},
};

const reducer = (state: State, action: { type: string, payload?: Checked, possibleKeys: Checked}) => {
  const { type, payload, possibleKeys } = action;

  switch (type) {
    case 'SET_ALL_STRATEGY':
      return ({
        strategy: 'ALL',
        checked: { ...possibleKeys, searchFilterQuery: false, queryWithReplacedParams: false },
      });
    case 'SET_EXACT_STRATEGY':
      return ({
        strategy: 'EXACT',
        checked: possibleKeys,
      });
    case 'SET_ROW_STRATEGY':
      return ({
        strategy: 'ROW',
        checked: { ...possibleKeys, columnValuePath: false, columnGroupBy: false },
      });
    case 'SET_COL_STRATEGY':
      return ({
        strategy: 'COL',
        checked: { ...possibleKeys, rowValuePath: false, rowGroupBy: false },
      });
    case 'SET_CUSTOM_STRATEGY':
      return ({
        strategy: 'CUSTOM',
        checked: state.checked,
      });
    case 'UPDATE_CHECKED_ITEMS':
      return ({
        strategy: 'CUSTOM',
        checked: { ...state.checked, ...payload },
      });
    default:
      return state;
  }
};

const CheckboxLabel = ({ itemKey, value }: { itemKey: ItemKey, value: string | number}) => (
  <span>
    <i>{`${labels[itemKey]}: `}</i>
    <b>{value}</b>
  </span>
);

const CreateEventDefinitionModal = ({ modalData }: { modalData: ModalData }) => {
  const [show, setShow] = useState(true);
  const [{ strategy, checked }, dispatch] = useReducer(reducer, initState);
  const dispatchWithData = useCallback(({ type, payload }: { type: string, payload?: Checked }) => {
    const possibleKeys = mapValues(modalData, (v) => !!v);

    return dispatch({ type, payload, possibleKeys });
  }, [modalData]);

  useEffect(() => {
    dispatchWithData({ type: 'SET_EXACT_STRATEGY' });
  }, [dispatchWithData]);

  const onCheckboxChange = useCallback((updates) => {
    dispatchWithData({ type: 'UPDATE_CHECKED_ITEMS', payload: updates });
  }, [dispatchWithData]);

  const onStrategyChange = useCallback((e) => {
    dispatchWithData({ type: `SET_${e.target.value}_STRATEGY` });
  }, [dispatchWithData]);
  const aggregationChecks = useMemo<Partial<Checked>>(() => pick(checked, aggregationGroup), [checked]);
  const searchChecks = useMemo<Partial<Checked>>(() => pick(checked, searchGroup), [checked]);
  const restChecks = useMemo<Partial<Checked>>(() => omit(checked, [...searchGroup, ...aggregationGroup]), [checked]);

  const aggregationLabels = useMemo<{ [name: string]: JSX.Element }>(() => {
    return mapValues(pick(modalData, aggregationGroup), (value, key: ItemKey) => {
      return <CheckboxLabel itemKey={key} value={value} />;
    });
  }, [modalData]);
  const searchLabels = useMemo<{ [name: string]: JSX.Element }>(() => {
    return mapValues(pick(modalData, searchGroup), (value, key: ItemKey) => (
      <CheckboxLabel itemKey={key} value={value} />
    ));
  }, [modalData]);

  const restLabels = useMemo<{ [name: string]: JSX.Element }>(() => {
    return mapValues(omit(modalData, [...aggregationGroup, ...searchGroup]), (value, key: ItemKey) => (
      <CheckboxLabel itemKey={key} value={value} />
    ));
  }, [modalData]);

  return (
    <Modal onHide={() => {}} show={show}>
      <Modal.Header closeButton>
        <Modal.Title>Modal Title</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <RadioSection strategy={strategy} onChange={onStrategyChange} />
        <CheckBoxGroup onChange={onCheckboxChange} groupLabel="Aggregation" checked={aggregationChecks} labels={aggregationLabels} />
        <CheckBoxGroup onChange={onCheckboxChange} groupLabel="Search query" checked={searchChecks} labels={searchLabels} />
        {
          Object.entries(restChecks).map(([key, isChecked]) => (
            <Checkbox checked={isChecked} onChange={() => onCheckboxChange({ [key]: !isChecked })}>
              {restLabels[key]}
            </Checkbox>
          ))
        }
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => {}}>
          Cancel
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateEventDefinitionModal;
