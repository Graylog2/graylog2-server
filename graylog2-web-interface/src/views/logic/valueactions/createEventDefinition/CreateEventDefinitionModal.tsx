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
import React, { useMemo, useCallback } from 'react';
import mapValues from 'lodash/mapValues';
import pick from 'lodash/pick';
import omit from 'lodash/omit';
import isEmpty from 'lodash/isEmpty';
import styled from 'styled-components';

import Store from 'logic/local-storage/Store';
import { Modal, Button } from 'components/bootstrap';
import type {
  ItemKey,
  Checked,
  ModalData, MappedData, StrategyId,
} from 'views/logic/valueactions/createEventDefinition/types';
import CheckBoxGroup from 'views/logic/valueactions/createEventDefinition/CheckBoxGroup';
import {
  aggregationGroup,
  searchGroup,
  labels,
} from 'views/logic/valueactions/createEventDefinition/Constants';
import RadioSection from 'views/logic/valueactions/createEventDefinition/RadioSection';
import { ExpandableList, ExpandableListItem, Icon } from 'components/common';
import useLocalStorageConfigData from 'views/logic/valueactions/createEventDefinition/hooks/useLocalStorageConfigData';
import Routes from 'routing/Routes';
import useModalReducer from 'views/logic/valueactions/createEventDefinition/hooks/useModalReducer';
import generateId from 'logic/generateId';

const Container = styled.div`
  margin-top: 10px;
`;
const CheckboxLabel = ({ itemKey, value }: { itemKey: ItemKey, value: string | number}) => (
  <span>
    <i>{`${labels[itemKey]}: `}</i>
    <b>{value}</b>
  </span>
);

const CreateEventDefinitionModal = ({ modalData, mappedData, show, onClose }: { mappedData: MappedData, modalData: ModalData, show: boolean, onClose: () => void }) => {
  const [{ strategy, checked, showDetails }, dispatchWithData] = useModalReducer(modalData);
  const localStorageConfig = useLocalStorageConfigData({ mappedData, checked });
  const sessionId = useMemo(() => `cedfv-${generateId()}`, []);
  const eventDefinitionCreationUrl = `${Routes.ALERTS.DEFINITIONS.CREATE}?step=condition&session-id=${sessionId}`;

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

  const aggregationLabels = useMemo<{ [name: string]: JSX.Element }>(() => mapValues(pick(modalData, aggregationGroup), (value, key: ItemKey) => <CheckboxLabel itemKey={key} value={value} />), [modalData]);
  const searchLabels = useMemo<{ [name: string]: JSX.Element }>(() => mapValues(pick(modalData, searchGroup), (value, key: ItemKey) => (
    <CheckboxLabel itemKey={key} value={value} />
  )), [modalData]);

  const restLabels = useMemo<{ [name: string]: JSX.Element }>(() => mapValues(omit(modalData, [...aggregationGroup, ...searchGroup]), (value, key: ItemKey) => (
    <CheckboxLabel itemKey={key} value={value} />
  )), [modalData]);

  const strategyAvailabilities = useMemo<{[name in StrategyId]: boolean}>(() => ({
    ALL: true,
    ROW: !!mappedData?.rowValuePath?.length,
    COL: !!mappedData?.columnValuePath?.length,
    CUSTOM: true,
    EXACT: true,
  }), [mappedData?.columnValuePath?.length, mappedData?.rowValuePath?.length]);

  const onContinueConfigurationClick = useCallback(() => {
    Store.set(sessionId, localStorageConfig);
    onClose();
  }, [sessionId, localStorageConfig, onClose]);

  return (
    <Modal onHide={onClose} show={show}>
      <Modal.Header closeButton>
        <Modal.Title>Configure new event definition</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <RadioSection strategyAvailabilities={strategyAvailabilities} strategy={strategy} onChange={onStrategyChange} />
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDetailsOpen}>
          <Icon name={`arrow_${showDetails ? 'drop_down' : 'right'}`} />&nbsp;
          {showDetails ? 'Hide strategy details' : 'Show strategy details'}
        </Button>
        {
          showDetails && (
          <Container>
            <ExpandableList>
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
                <ExpandableListItem key={key}
                                    checked={isChecked}
                                    onChange={() => onCheckboxChange({ [key]: !isChecked })}
                                    header={restLabels[key]}
                                    padded={false}
                                    expandable={false} />
              ))
            }
            </ExpandableList>
          </Container>
          )
        }
      </Modal.Body>
      <Modal.Footer>
        <Button bsStyle="primary" onClick={onContinueConfigurationClick} href={eventDefinitionCreationUrl} target="_blank">
          Continue configuration
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateEventDefinitionModal;
