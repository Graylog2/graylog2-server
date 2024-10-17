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
import { PluginStore } from 'graylog-web-plugin/plugin';
import defaultTo from 'lodash/defaultTo';
import get from 'lodash/get';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Select } from 'components/common';
import { Clearfix, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { HelpPanel } from 'components/event-definitions/common/HelpPanel';
import type User from 'logic/users/User';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';

import styles from './EventConditionForm.css';

import type { EventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';
import { SYSTEM_EVENT_DEFINITION_TYPE } from '../constants';

type Props = {
  action?: 'create' | 'edit'
  entityTypes?: any
  eventDefinition: EventDefinition,
  validation: {
    errors: {
      config?: unknown,
      title?: string,
    }
  },
  currentUser: User,
  onChange: (name: string, newConfig: EventDefinition['config']) => void,
  canEdit: boolean,
}

const EventConditionForm = ({ action = 'create', entityTypes, eventDefinition, validation, currentUser, onChange, canEdit }: Props) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const getConditionPlugin = (type: string) => {
    if (type === undefined) {
      return undefined;
    }

    return PluginStore.exports('eventDefinitionTypes').find((eventDefinitionType) => eventDefinitionType.type === type);
  };

  const sortedEventDefinitionTypes = (): any => (PluginStore.exports('eventDefinitionTypes') as any).sort((eventDefinitionType1, eventDefinitionType2) => {
    // Try to sort by given sort order and displayName if possible, otherwise do it by displayName
    const eventDefinitionType1Order = eventDefinitionType1.sortOrder;
    const eventDefinitionType2Order = eventDefinitionType2.sortOrder;

    if (eventDefinitionType1Order !== undefined || eventDefinitionType2Order !== undefined) {
      const sort = defaultTo(eventDefinitionType1Order, Number.MAX_SAFE_INTEGER) - defaultTo(eventDefinitionType2Order, Number.MAX_SAFE_INTEGER);

      if (sort !== 0) {
        return sort;
      }
    }

    return naturalSort(eventDefinitionType1.displayName, eventDefinitionType2.displayName);
  });

  const formattedEventDefinitionTypes = () => sortedEventDefinitionTypes()
    .map((type) => ({ label: type.displayName, value: type.type }));

  const handleEventDefinitionTypeChange = (nextType: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.TYPE_SELECTED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-definition-condition',
      app_action_value: 'type-select',
      condition_type: nextType,
    });

    const conditionPlugin = getConditionPlugin(nextType);
    const defaultConfig = conditionPlugin?.defaultConfig || {} as EventDefinition['config'];

    onChange('config', { ...defaultConfig, type: nextType });
  };

  const renderConditionTypeDescriptions = () => {
    const typeDescriptions = sortedEventDefinitionTypes()
      .map((type) => (
        <React.Fragment key={type.type}>
          <dt>{type.displayName}</dt>
          <dd>{type.description || 'No description available.'}</dd>
        </React.Fragment>
      ));

    return <dl>{typeDescriptions}</dl>;
  };

  const disabledSelect = () => !formattedEventDefinitionTypes().some((edt) => eventDefinition.config.type === edt.value) && action === 'edit';
  const onlyFilters = () => eventDefinition._scope === 'ILLUMINATE' && action === 'edit';
  const isSigma = () => eventDefinition.config.type === 'sigma-v1' && action === 'edit';

  const eventDefinitionType = getConditionPlugin(eventDefinition.config.type);
  const isSystemEventDefinition = eventDefinition.config.type === SYSTEM_EVENT_DEFINITION_TYPE;
  const canEditCondition = canEdit && !isSystemEventDefinition;

  const eventDefinitionTypeComponent = eventDefinitionType?.formComponent
    ? React.createElement(eventDefinitionType.formComponent, {
      action,
      entityTypes,
      currentUser,
      validation,
      eventDefinition,
      onChange,
      key: eventDefinition.id,
    })
    : null;

  return (
    <Row>
      <Col md={7} lg={6}>
        <h2 className={commonStyles.title}>Event Condition</h2>

        {!canEditCondition ? (
          <p>
            The conditions of this event definition type cannot be edited.
          </p>
        ) : (
          <>
            <p>
              Configure how Graylog should create Events of this kind. You can later use those Events as input on other
              Conditions, making it possible to build powerful Conditions based on others.
            </p>
            <FormGroup validationState={validation.errors.config ? 'error' : null}>
              <ControlLabel htmlFor="event-condition-type-select">Condition Type</ControlLabel>
              <Select placeholder="Select a Condition Type"
                      inputId="event-condition-type-select"
                      options={formattedEventDefinitionTypes()}
                      value={eventDefinition.config.type}
                      onChange={handleEventDefinitionTypeChange}
                      clearable={false}
                      disabled={disabledSelect() || onlyFilters() || isSigma()}
                      required />
              <HelpBlock>
                {get(validation, 'errors.config[0]', 'Choose the type of Condition for this Event.')}
              </HelpBlock>
            </FormGroup>
          </>
        )}
      </Col>

      {canEditCondition && !disabledSelect() && (
        <>
          <Col md={5} lg={5} lgOffset={1}>
            <HelpPanel className={styles.conditionTypesInfo}
                       title="Available Conditions">
              {renderConditionTypeDescriptions()}
            </HelpPanel>
          </Col>
          <Clearfix />

          {eventDefinitionTypeComponent && (
            <>
              <hr className={styles.hr} />
              <Col md={12}>
                {eventDefinitionTypeComponent}
              </Col>
            </>
          )}
        </>
      )}
    </Row>
  );
};

export default EventConditionForm;
