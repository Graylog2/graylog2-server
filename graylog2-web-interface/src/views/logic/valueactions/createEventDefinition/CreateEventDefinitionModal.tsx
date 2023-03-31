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
import React, { useMemo, useReducer, useCallback, useEffect } from 'react';
import mapValues from 'lodash/mapValues';
import pick from 'lodash/pick';
import omit from 'lodash/omit';
import isEmpty from 'lodash/isEmpty';
import objectHas from 'lodash/has';

import { Checkbox, Modal, Button } from 'components/bootstrap';
import type {
  ItemKey,
  Checked,
  State,
  ModalData, MappedData,
} from 'views/logic/valueactions/createEventDefinition/types';
import CheckBoxGroup from 'views/logic/valueactions/createEventDefinition/CheckBoxGroup';
import {
  aggregationGroup,
  searchGroup,
  labels,
} from 'views/logic/valueactions/createEventDefinition/Constants';
import RadioSection from 'views/logic/valueactions/createEventDefinition/RadioSection';
import { Icon } from 'components/common';
import { Link } from 'components/common/router';
import useUrlConfigData from 'views/logic/valueactions/createEventDefinition/hooks/useUrlConfigData';
import Routes from 'routing/Routes';

const initState: State = {
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
        checked: { ...possibleKeys, searchFilterQuery: false, queryWithReplacedParams: false },
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

const CheckboxLabel = ({ itemKey, value }: { itemKey: ItemKey, value: string | number}) => (
  <span>
    <i>{`${labels[itemKey]}: `}</i>
    <b>{value}</b>
  </span>
);

const CreateEventDefinitionModal = ({ modalData, mappedData, show, onClose }: { mappedData: MappedData, modalData: ModalData, show: boolean, onClose: () => void }) => {
  const [{ strategy, checked, showDetails }, dispatch] = useReducer(reducer, initState);
  const urlConfig = useUrlConfigData({ mappedData, checked });
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
  const toggleDetailsOpen = useCallback(() => {
    dispatchWithData({ type: 'TOGGLE_SHOW_DETAILS' });
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

  const eventDefinitionCreationUrl = useMemo(() => {
    return `${Routes.ALERTS.DEFINITIONS.CREATE}?step=condition&config=${JSON.stringify(urlConfig)}`;
  }, [urlConfig]);

  return (
    <Modal onHide={onClose} show={show}>
      <Modal.Header closeButton>
        <Modal.Title>Modal Title</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <RadioSection strategy={strategy} onChange={onStrategyChange} />
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDetailsOpen}>
          <Icon name={`caret-${showDetails ? 'down' : 'right'}`} />&nbsp;
          {showDetails ? 'Hide strategy details' : 'Show strategy details'}
        </Button>
        {
          showDetails && (
          <div>
            {!isEmpty(aggregationChecks) && (
            <CheckBoxGroup onChange={onCheckboxChange}
                           groupLabel="Aggregation"
                           checked={aggregationChecks}
                           labels={aggregationLabels} />
            )}
            {!isEmpty(searchChecks) && (
            <CheckBoxGroup onChange={onCheckboxChange}
                           groupLabel="Search query"
                           checked={searchChecks}
                           labels={searchLabels} />
            )}
            {
              Object.entries(restChecks).map(([key, isChecked]) => (
                <Checkbox checked={isChecked} onChange={() => onCheckboxChange({ [key]: !isChecked })}>
                  {restLabels[key]}
                </Checkbox>
              ))
            }
          </div>
          )
        }
      </Modal.Body>
      <Modal.Footer>
        {/*
        <Button type="button" onClick={onClose}>
          Cancel
        </Button>
        <Button bsStyle="primary" to="/search">
          Go to event definition
        </Button>
        */}
        <Link onClick={onClose} to={eventDefinitionCreationUrl} target="_blank">Go to event definition</Link>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateEventDefinitionModal;
