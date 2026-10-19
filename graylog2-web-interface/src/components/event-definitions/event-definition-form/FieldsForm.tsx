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
import omit from 'lodash/omit';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { useTranslation } from 'react-i18next';

import { Alert, Col, Row } from 'components/bootstrap';
import EventKeyHelpPopover from 'components/event-definitions/common/EventKeyHelpPopover';
import type User from 'logic/users/User';
import HoverForHelp from 'components/common/HoverForHelp';

import FieldForm from './FieldForm';
import FieldsList from './FieldsList';
import TagsEditor from './TagsEditor';

import type { EventDefinition } from '../event-definitions-types';
import { isSystemEventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';

type Props = {
  currentUser: User;
  eventDefinition: EventDefinition;
  validation: {
    errors: {
      title?: string;
      field_spec?: string[];
      key_spec?: string[];
      tags?: string[] | string;
      tactics_techniques?: string[] | string;
    };
  };
  onChange: (name: string, value: unknown) => void;
  canEdit: boolean;
};

const FieldsForm = ({ currentUser, eventDefinition, validation, onChange, canEdit }: Props) => {
  const { t } = useTranslation('event_definitions');
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
    const nextFieldSpec =
      prevFieldName === fieldName
        ? cloneDeep(eventDefinition.field_spec)
        : omit(eventDefinition.field_spec, prevFieldName);

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

  const canEditCondition = canEdit && !isSystemEventDefinition(eventDefinition);
  const tacticsTechniquesEditorPlugin = PluginStore.exports('eventDefinitions.components.tacticsTechniquesEditor')[0];

  if (showFieldForm) {
    return (
      <FieldForm
        keys={eventDefinition.key_spec}
        fieldName={editField}
        config={editField ? eventDefinition.field_spec[editField] : undefined}
        onChange={addCustomField}
        onCancel={toggleFieldForm}
        currentUser={currentUser}
      />
    );
  }

  const fieldErrors = validation?.errors?.field_spec ?? [];
  const keyErrors = validation?.errors?.key_spec ?? [];
  const errors = [...fieldErrors, ...keyErrors];

  return (
    <Row>
      <Col md={12}>
        <h2 className={commonStyles.title}>
          {t('fields_form.tags')} <small>{t('fields_form.optional')}</small>
        </h2>
        <p>{t('fields_form.tags_description')}</p>
        <TagsEditor
          tags={eventDefinition.tags ?? []}
          onChange={(next) => onChange('tags', next)}
          disabled={!canEditCondition}
          error={
            Array.isArray(validation?.errors?.tags)
              ? validation.errors.tags.join(' ')
              : (validation?.errors?.tags ?? null)
          }
        />

        {tacticsTechniquesEditorPlugin ? (
          <tacticsTechniquesEditorPlugin.component
            value={eventDefinition.tactics_techniques ?? []}
            onChange={(next: string[]) => onChange('tactics_techniques', next)}
            disabled={!canEditCondition}
            error={
              Array.isArray(validation?.errors?.tactics_techniques)
                ? validation.errors.tactics_techniques.join(' ')
                : (validation?.errors?.tactics_techniques ?? null)
            }
          />
        ) : null}

        <h2 className={commonStyles.title}>
          {t('fields_form.event_fields')} <small>{t('fields_form.optional')}</small>
        </h2>

        {!canEditCondition ? (
          <p>{t('fields_form.not_editable')}</p>
        ) : (
          <>
            <p>{t('fields_form.description')}</p>

            {errors.length > 0 && (
              <Alert bsStyle="danger" className={commonStyles.validationSummary} title={t('fields_form.errors_title')}>
                <p>{t('fields_form.errors_description')}</p>
                <ul>
                  {errors.map((error) => (
                    <li key={error}>{error}</li>
                  ))}
                </ul>
              </Alert>
            )}

            {Object.keys(eventDefinition.field_spec).length > 0 && (
              <dl>
                <dt>
                  {t('summary_renders.keys')}
                  <HoverForHelp
                    title={t('field_form.more_about_keys')}
                    trigger={['click', 'hover']}
                    placement="right"
                    width={400}>
                    <EventKeyHelpPopover />
                  </HoverForHelp>
                </dt>
                <dd>
                  {eventDefinition.key_spec.length > 0
                    ? eventDefinition.key_spec.join(', ')
                    : t('fields_form.no_keys')}
                </dd>
              </dl>
            )}
            <FieldsList
              fields={eventDefinition.field_spec}
              validation={validation}
              keys={eventDefinition.key_spec}
              onAddFieldClick={toggleFieldForm}
              onEditFieldClick={toggleFieldForm}
              onRemoveFieldClick={removeCustomField}
            />
          </>
        )}
      </Col>
    </Row>
  );
};

export default FieldsForm;
