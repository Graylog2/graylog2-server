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
import { useMemo, useState } from 'react';
import styled from 'styled-components';
import upperFirst from 'lodash/upperFirst';
import toNumber from 'lodash/toNumber';
import toString from 'lodash/toString';
import { useMantineTheme } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';

import { Select } from 'components/common';
import { Button, Col, ControlLabel, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import * as FormsUtils from 'util/FormsUtils';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { EventDefinition } from '../event-definitions-types';
import { isSystemEventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';

const StyledRow = styled.div`
  display: flex;
  flex-direction: row;
  gap: ${({ theme }) => theme.spacings.lg};

  & .form-group {
    width: 100%;
  }
`;

const priorityOptions = Object.entries(EventDefinitionPriorityEnum.properties)
  .map(([key, value]) => ({
    value: key,
    label: upperFirst(value.name),
  }))
  .sort((a, b) => Number(b.value) - Number(a.value));

type Props = {
  eventDefinition: EventDefinition;
  eventDefinitionEventProcedure: string;
  validation: {
    errors: {
      title?: string;
    };
  };
  onChange: (name: string, value: string | number) => void;
  canEdit: boolean;
};

const EventDetailsForm = ({ eventDefinition, eventDefinitionEventProcedure, validation, onChange, canEdit }: Props) => {
  const theme = useMantineTheme();
  const ltXl = useMediaQuery(`(min-width: ${theme.breakpoints.xl}`);
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();
  const [showAddEventProcedureForm, setShowAddEventProcedureForm] = useState<boolean>(false);
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  const readOnly = useMemo(
    () => !canEdit || isSystemEventDefinition(eventDefinition) || eventDefinition.config.type === 'sigma-v1',
    [canEdit, eventDefinition],
  );
  const showEventProcedureSummary = useMemo(
    () => !!eventDefinitionEventProcedure && !showAddEventProcedureForm && validSecurityLicense,
    [eventDefinitionEventProcedure, showAddEventProcedureForm, validSecurityLicense],
  );
  const showAddNewEventProcedure = useMemo(
    () => !eventDefinitionEventProcedure && !showAddEventProcedureForm && !readOnly && validSecurityLicense,
    [eventDefinitionEventProcedure, showAddEventProcedureForm, readOnly, validSecurityLicense],
  );

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;

    onChange(name, String(FormsUtils.getValueFromInput(event.target)));
  };

  const handlePriorityChange = (nextPriority: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DETAILS.PRIORITY_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-definition-details',
      app_action_value: 'priority-select',
      priority: priorityOptions[toNumber(nextPriority) - 1]?.label,
    });

    onChange('priority', toNumber(nextPriority));
  };

  const PluggableEventProcedureForm = usePluginEntities('views.components.eventProcedureForm')?.[0]?.component;
  const PluggableEventProcedureSummary = usePluginEntities('views.components.eventProcedureSummary')?.[0]?.component;

  return (
    <Row>
      <Col md={12} lg={6}>
        <h2 className={commonStyles.title}>Event Details</h2>
        <fieldset>
          <StyledRow>
            <Input
              id="event-definition-title"
              name="title"
              label="Title"
              type="text"
              bsStyle={validation.errors.title ? 'error' : null}
              help={validation?.errors?.title?.[0] ?? 'Title for this Event Definition.'}
              value={eventDefinition.title}
              onChange={handleChange}
              readOnly={readOnly}
              required
            />

            <FormGroup controlId="event-definition-priority">
              <ControlLabel>Priority</ControlLabel>
              <Select
                options={priorityOptions}
                value={toString(eventDefinition.priority)}
                onChange={handlePriorityChange}
                clearable={false}
                disabled={readOnly}
                required
              />
              <HelpBlock>Choose the priority for Events created from this Definition.</HelpBlock>
            </FormGroup>
          </StyledRow>

          <Input
            id="event-definition-event-summary-template"
            name="event_summary_template"
            label="Event Summary Template"
            type="text"
            help="Template used to generate the Event and Alert summaries."
            value={eventDefinition.event_summary_template}
            onChange={handleChange}
            readOnly={readOnly}
          />

          <Input
            id="event-definition-description"
            name="description"
            label={
              <span>
                Description <small className="text-muted">(Optional)</small>
              </span>
            }
            type="textarea"
            help="Longer description for this Event Definition."
            value={eventDefinition.description}
            onChange={handleChange}
            readOnly={readOnly}
            rows={2}
          />
          {showAddEventProcedureForm && (
            <PluggableEventProcedureForm
              eventProcedureId={eventDefinitionEventProcedure}
              remediationSteps={eventDefinition?.remediation_steps}
              onClose={() => setShowAddEventProcedureForm(false)}
              onSave={(eventProcedureId) => {
                onChange('event_procedure', eventProcedureId);
                setShowAddEventProcedureForm(false);
              }}
              onRemove={() => {
                onChange('event_procedure', null);
                setShowAddEventProcedureForm(false);
              }}
            />
          )}
          {showEventProcedureSummary && (
            <Col>
              <ControlLabel>Event Procedure Summary</ControlLabel>
              <PluggableEventProcedureSummary
                eventProcedureId={eventDefinitionEventProcedure}
                canEdit={!readOnly}
                onEdit={() => setShowAddEventProcedureForm(true)}
                onRemove={() => onChange('event_procedure', null)}
                row={ltXl}
              />
            </Col>
          )}
          {showAddNewEventProcedure && (
            <>
              <ControlLabel>Event Procedure Summary</ControlLabel>
              <p>This Event Definition does not have any Event Procedures yet.</p>
              <Button bsStyle="primary" onClick={() => setShowAddEventProcedureForm(true)}>
                Add Event Procedure
              </Button>
            </>
          )}
        </fieldset>
      </Col>
    </Row>
  );
};

export default EventDetailsForm;
