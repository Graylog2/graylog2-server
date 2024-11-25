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
import * as React from 'react';
import { useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import omit from 'lodash/omit';

import { Alert, Col, Row } from 'components/bootstrap';
import EventKeyHelpPopover from 'components/event-definitions/common/EventKeyHelpPopover';
import type User from 'logic/users/User';
import HoverForHelp from 'components/common/HoverForHelp';

import FieldForm from './FieldForm';
import FieldsList from './FieldsList';

import type { EventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';

type Props = {
  currentUser: User,
  eventDefinition: EventDefinition,
  validation: {
    errors: {
      title?: string,
    }
  },
  onChange: (name: string, value: unknown) => void,
  canEdit: boolean,
}

const FieldsForm = ({ currentUser, eventDefinition, validation, onChange, canEdit }: Props) => {
  const [editField, setEditField] = useState<string | undefined>(undefined);
  const [showFieldForm, setShowFieldForm] = useState<boolean>(false);

  const removeCustomField = (fieldName) => {
    const nextFieldSpec = omit(eventDefinition.field_spec, fieldName);

    onChange('field_spec', nextFieldSpec);

    // Filter out all non-existing field names from key_spec
    const fieldNames = Object.keys(nextFieldSpec);
    const nextKeySpec = eventDefinition.key_spec.filter((key) => fieldNames.includes(key));

    onChange('key_spec', nextKeySpec);
  };

  const toggleFieldForm = (fieldName?: string) => {
    setEditField(showFieldForm ? undefined : fieldName);
    setShowFieldForm(!showFieldForm);
  };

  const addCustomField = (prevFieldName, fieldName, config, isKey, keyPosition) => {
    const nextFieldSpec = (prevFieldName === fieldName
      ? cloneDeep(eventDefinition.field_spec)
      : omit(eventDefinition.field_spec, prevFieldName));

    nextFieldSpec[fieldName] = config;
    onChange('field_spec', nextFieldSpec);

    // Filter out all non-existing field names from key_spec and the current field name
    const fieldNames = Object.keys(nextFieldSpec);
    let nextKeySpec = eventDefinition.key_spec.filter((key) => fieldNames.includes(key) && key !== fieldName);

    if (isKey) {
      // Add key to its new position
      nextKeySpec = [...nextKeySpec.slice(0, keyPosition), fieldName, ...nextKeySpec.slice(keyPosition)];
    }

    onChange('key_spec', nextKeySpec);

    toggleFieldForm();
  };

  const isSystemEventDefinition = eventDefinition.config.type === 'system-notifications-v1';
  const canEditCondition = canEdit && !isSystemEventDefinition;

  if (showFieldForm) {
    return (
      <FieldForm keys={eventDefinition.key_spec}
                 fieldName={editField}
                 config={editField ? eventDefinition.field_spec[editField] : undefined}
                 onChange={addCustomField}
                 onCancel={toggleFieldForm}
                 currentUser={currentUser} />
    );
  }

  const fieldErrors = get(validation, 'errors.field_spec', []);
  const keyErrors = get(validation, 'errors.key_spec', []);
  const errors = [...fieldErrors, ...keyErrors];

  return (
    <Row>
      <Col md={12}>
        <h2 className={commonStyles.title}>Event Fields <small>(optional)</small></h2>

        {!canEditCondition ? (
          <p>
            The event fields of this event definition type cannot be edited.
          </p>
        ) : (
          <>
            <p>
              Include additional information in Events generated from this Event Definition by adding custom Fields. That
              can help you search Events or having more context when receiving Notifications.
            </p>

            {errors.length > 0 && (
              <Alert bsStyle="danger" className={commonStyles.validationSummary} title="Fields with errors">
                <p>Please correct the following errors before saving this Event Definition:</p>
                <ul>
                  {errors.map((error) => <li key={error}>{error}</li>)}
                </ul>
              </Alert>
            )}

            {Object.keys(eventDefinition.field_spec).length > 0 && (
              <dl>
                <dt>
                  Keys
                  <HoverForHelp title="More about Event Keys" trigger={['click', 'hover']} placement="right">
                    <EventKeyHelpPopover />
                  </HoverForHelp>
                </dt>
                <dd>{eventDefinition.key_spec.length > 0 ? eventDefinition.key_spec.join(', ') : 'No Keys configured yet.'}</dd>
              </dl>
            )}
            <FieldsList fields={eventDefinition.field_spec}
                        validation={validation}
                        keys={eventDefinition.key_spec}
                        onAddFieldClick={toggleFieldForm}
                        onEditFieldClick={toggleFieldForm}
                        onRemoveFieldClick={removeCustomField} />
          </>
        )}
      </Col>
    </Row>
  );
};

export default FieldsForm;
