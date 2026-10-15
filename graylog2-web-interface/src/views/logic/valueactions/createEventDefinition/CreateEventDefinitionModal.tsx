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
import React, { useMemo, useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  ModalData,
  MappedData,
  StrategyId,
} from 'views/logic/valueactions/createEventDefinition/types';
import CheckBoxGroup from 'views/logic/valueactions/createEventDefinition/CheckBoxGroup';
import { aggregationGroup, searchGroup, labels } from 'views/logic/valueactions/createEventDefinition/Constants';
import RadioSection from 'views/logic/valueactions/createEventDefinition/RadioSection';
import { ExpandableList, Icon, ExpandableCheckboxListItem } from 'components/common';
import useLocalStorageConfigData from 'views/logic/valueactions/createEventDefinition/hooks/useLocalStorageConfigData';
import Routes from 'routing/Routes';
import useModalReducer from 'views/logic/valueactions/createEventDefinition/hooks/useModalReducer';
import generateId from 'logic/generateId';

const Container = styled.div`
  margin-top: 10px;
`;
const CheckboxLabel = ({ itemKey, value }: { itemKey: ItemKey; value: string | number }) => {
  const { t } = useTranslation('search');

  return (
    <span>
      <i>{t('create_event_definition.item_label', { label: t(labels[itemKey]) })}</i>
      <b>{value}</b>
    </span>
  );
};

const SECTION_KEYS = {
  aggregation: 'aggregation',
  search: 'search',
} as const;

const CreateEventDefinitionModal = ({
  modalData,
  mappedData,
  show,
  onClose,
}: {
  mappedData: MappedData;
  modalData: ModalData;
  show: boolean;
  onClose: () => void;
}) => {
  const { t } = useTranslation('search');
  const [expandedSections, setExpandedSections] = useState<Array<string>>(Object.values(SECTION_KEYS));
  const [{ strategy, checked, showDetails }, dispatchWithData] = useModalReducer(modalData);
  const localStorageConfig = useLocalStorageConfigData({ mappedData, checked });
  const sessionId = useMemo(() => `cedfv-${generateId()}`, []);
  const eventDefinitionCreationUrl = `${Routes.ALERTS.DEFINITIONS.CREATE}?step=condition&session-id=${sessionId}`;

  const onCheckboxChange = useCallback(
    (updates) => {
      dispatchWithData({ type: 'UPDATE_CHECKED_ITEMS', payload: updates });
    },
    [dispatchWithData],
  );

  const onStrategyChange = useCallback(
    (e) => {
      dispatchWithData({ type: `SET_${e.target.value}_STRATEGY` });
    },
    [dispatchWithData],
  );
  const toggleDetailsOpen = useCallback(() => {
    dispatchWithData({ type: 'TOGGLE_SHOW_DETAILS' });
  }, [dispatchWithData]);

  const aggregationChecks = useMemo<Partial<Checked>>(() => pick(checked, aggregationGroup), [checked]);
  const searchChecks = useMemo<Partial<Checked>>(() => pick(checked, searchGroup), [checked]);
  const restChecks = useMemo<Partial<Checked>>(() => omit(checked, [...searchGroup, ...aggregationGroup]), [checked]);

  const aggregationLabels = useMemo<{ [name: string]: JSX.Element }>(
    () =>
      mapValues(pick(modalData, aggregationGroup), (value, key: ItemKey) => (
        <CheckboxLabel itemKey={key} value={value} />
      )),
    [modalData],
  );
  const searchLabels = useMemo<{ [name: string]: JSX.Element }>(
    () =>
      mapValues(pick(modalData, searchGroup), (value, key: ItemKey) => <CheckboxLabel itemKey={key} value={value} />),
    [modalData],
  );

  const restLabels = useMemo<{ [name: string]: JSX.Element }>(
    () =>
      mapValues(omit(modalData, [...aggregationGroup, ...searchGroup]), (value, key: ItemKey) => (
        <CheckboxLabel itemKey={key} value={value} />
      )),
    [modalData],
  );

  const strategyAvailabilities = useMemo<{ [name in StrategyId]: boolean }>(
    () => ({
      ALL: true,
      ROW: !!mappedData?.rowValuePath?.length,
      COL: !!mappedData?.columnValuePath?.length,
      CUSTOM: true,
      EXACT: true,
    }),
    [mappedData?.columnValuePath?.length, mappedData?.rowValuePath?.length],
  );

  const onContinueConfigurationClick = useCallback(() => {
    Store.set(sessionId, localStorageConfig);
    onClose();
  }, [sessionId, localStorageConfig, onClose]);

  return (
    <Modal onHide={onClose} show={show}>
      <Modal.Header>
        <Modal.Title>{t('create_event_definition.title')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <RadioSection strategyAvailabilities={strategyAvailabilities} strategy={strategy} onChange={onStrategyChange} />
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDetailsOpen}>
          <Icon name={`arrow_${showDetails ? 'drop_down' : 'right'}`} />
          &nbsp;
          {showDetails ? t('create_event_definition.hide_details') : t('create_event_definition.show_details')}
        </Button>
        {showDetails && (
          <Container>
            <ExpandableList
              value={expandedSections}
              onChange={(newExpandedSections) => setExpandedSections(newExpandedSections)}>
              {!isEmpty(aggregationChecks) && (
                <CheckBoxGroup
                  onChange={onCheckboxChange}
                  groupKey={SECTION_KEYS.aggregation}
                  groupLabel={t('create_event_definition.sections.aggregation')}
                  checked={aggregationChecks}
                  labels={aggregationLabels}
                />
              )}
              {!isEmpty(searchChecks) && (
                <CheckBoxGroup
                  onChange={onCheckboxChange}
                  groupKey={SECTION_KEYS.search}
                  groupLabel={t('create_event_definition.sections.search')}
                  checked={searchChecks}
                  labels={searchLabels}
                />
              )}
              {Object.entries(restChecks).map(([key, isChecked]) => (
                <ExpandableCheckboxListItem
                  key={key}
                  value={key}
                  checked={isChecked}
                  onChange={() => onCheckboxChange({ [key]: !isChecked })}
                  header={restLabels[key]}
                  expandable={false}
                />
              ))}
            </ExpandableList>
          </Container>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button
          bsStyle="primary"
          onClick={onContinueConfigurationClick}
          href={eventDefinitionCreationUrl}
          target="_blank">
          {t('create_event_definition.continue')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateEventDefinitionModal;
