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
import lodash from 'lodash';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Select } from 'components/common';
import { Clearfix, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { HelpPanel } from 'components/event-definitions/common/HelpPanel';

import styles from './EventConditionForm.css';

import commonStyles from '../common/commonStyles.css';

import type { EventDefinition } from 'logic/alerts/types';
import type { UserJSON } from 'logic/users/User';

type Props = {
  eventDefinition: EventDefinition,
  currentUser: UserJSON,
  validation: { errors: { [key: string]: Array<string> } },
  onChange: (name: string, newConfig: EventDefinition['config']) => void,
  action: string,
};

const EventConditionForm = ({ eventDefinition, currentUser, validation, onChange, action }: Props) => {
  const getConditionPlugin = (type: string | undefined) => {
    if (type === undefined) {
      return undefined;
    }

    return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || undefined;
  };

  const sortedEventDefinitionTypes = () => {
    return PluginStore.exports('eventDefinitionTypes').sort((edt1, edt2) => {
      // Try to sort by given sort order and displayName if possible, otherwise do it by displayName
      const edt1Order = edt1.sortOrder;
      const edt2Order = edt2.sortOrder;

      if (edt1Order !== undefined || edt2Order !== undefined) {
        const sort = lodash.defaultTo(edt1Order, Number.MAX_SAFE_INTEGER) - lodash.defaultTo(edt2Order, Number.MAX_SAFE_INTEGER);

        if (sort !== 0) {
          return sort;
        }
      }

      return naturalSort(edt1.displayName, edt2.displayName);
    });
  };

  const formattedEventDefinitionTypes = () => {
    return sortedEventDefinitionTypes()
      .map((type) => ({ label: type.displayName, value: type.type }));
  };

  const handleEventDefinitionTypeChange = (nextType: string) => {
    const conditionPlugin = getConditionPlugin(nextType);
    const defaultConfig = conditionPlugin?.defaultConfig || {} as EventDefinition['config'];

    onChange('config', { ...defaultConfig, type: nextType });
  };

  const renderConditionTypeDescriptions = () => {
    const typeDescriptions = sortedEventDefinitionTypes()
      .map((type) => {
        return (
          <React.Fragment key={type.type}>
            <dt>{type.displayName}</dt>
            <dd>{type.description || 'No description available.'}</dd>
          </React.Fragment>
        );
      });

    return <dl>{typeDescriptions}</dl>;
  };

  const disabledSelect = () => {
    return !formattedEventDefinitionTypes().some((edt) => eventDefinition.config.type === edt.value) && action === 'edit';
  };

  const eventDefinitionType = getConditionPlugin(eventDefinition.config.type);

  const eventDefinitionTypeComponent = eventDefinitionType?.formComponent
    ? React.createElement<React.ComponentProps<any>>(eventDefinitionType.formComponent, {
      eventDefinition: eventDefinition,
      currentUser: currentUser,
      validation: validation,
      onChange: onChange,
      action: action,
      key: eventDefinition.id,
    })
    : null;

  return (
    <Row>
      <Col md={7} lg={6}>
        <h2 className={commonStyles.title}>Event Condition</h2>

        <p>
          Configure how Graylog should create Events of this kind. You can later use those Events as input on other
          Conditions, making it possible to build powerful Conditions based on others.
        </p>

        <FormGroup controlId="event-definition-priority" validationState={validation.errors.config ? 'error' : null}>
          <ControlLabel>Condition Type</ControlLabel>
          <Select placeholder="Select a Condition Type"
                  options={formattedEventDefinitionTypes()}
                  value={eventDefinition.config.type}
                  onChange={handleEventDefinitionTypeChange}
                  clearable={false}
                  disabled={disabledSelect()}
                  required />
          <HelpBlock>
            {lodash.get(validation, 'errors.config[0]', 'Choose the type of Condition for this Event.')}
          </HelpBlock>
        </FormGroup>
      </Col>
      {!disabledSelect() && (
        <Col md={5} lg={5} lgOffset={1}>
          <HelpPanel className={styles.conditionTypesInfo}
                     title="Available Conditions">
            {renderConditionTypeDescriptions()}
          </HelpPanel>
        </Col>
      )}
      <Clearfix />

      {eventDefinitionTypeComponent && (
        <>
          <hr className={styles.hr} />
          <Col md={12}>
            {eventDefinitionTypeComponent}
          </Col>
        </>
      )}
    </Row>
  );
};

export default EventConditionForm;
