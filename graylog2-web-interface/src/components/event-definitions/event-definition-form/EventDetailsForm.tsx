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
import get from 'lodash/get';
import map from 'lodash/map';
import upperFirst from 'lodash/upperFirst';
import toNumber from 'lodash/toNumber';
import toString from 'lodash/toString';

import { Select } from 'components/common';
import { MarkdownEditor, MarkdownPreview } from 'components/common/MarkdownEditor';
import { Button, Col, ControlLabel, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import usePluginEntities from 'hooks/usePluginEntities';
import * as FormsUtils from 'util/FormsUtils';
import AppConfig from 'util/AppConfig';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { EventDefinition } from '../event-definitions-types';
import { isSystemEventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';

const priorityOptions = map(EventDefinitionPriorityEnum.properties, (value, key) => ({
  value: key,
  label: upperFirst(value.name),
})).sort((a, b) => Number(b.value) - Number(a.value));

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
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();
  const [showAddEventProcedureForm, setShowAddEventProcedureForm] = React.useState(false);
  const pluggableEventProcedureForm = usePluginEntities('views.components.eventProcedureForm');
  const isEventProceduresEnabled = AppConfig.isFeatureEnabled('show_event_procedures');

  const onSave = (eventProcedureId: string) => {
    onChange('event_procedure', eventProcedureId);
  };

  const eventProcedureForm = React.useMemo(
    () => pluggableEventProcedureForm.map(({ component: PluggableEventProcedureForm }) => (
      <PluggableEventProcedureForm
        eventDefinition={eventDefinition}
        eventDefinitionEventProcedure={eventDefinitionEventProcedure}
        onClose={() => setShowAddEventProcedureForm(false)}
        onSave={onSave}
        canEdit={canEdit}
      />
    )),
    [pluggableEventProcedureForm, eventDefinition, eventDefinitionEventProcedure, onSave, canEdit],
  );

  const handleChange = (event) => {
    const { name } = event.target;

    onChange(name, FormsUtils.getValueFromInput(event.target));
  };

  const handlePriorityChange = (nextPriority) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DETAILS.PRIORITY_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-definition-details',
      app_action_value: 'priority-select',
      priority: priorityOptions[toNumber(nextPriority) - 1]?.label,
    });

    onChange('priority', toNumber(nextPriority));
  };

  const readOnly = !canEdit || isSystemEventDefinition(eventDefinition) || eventDefinition.config.type === 'sigma-v1';
  const hasEventProcedure = !!eventDefinitionEventProcedure;

  const renderEventProcedure = () => {
    if (isEventProceduresEnabled) {
      return (
        <>
          {(hasEventProcedure || showAddEventProcedureForm) ? (
            <>
              {
                eventProcedureForm.map((form) => (
                  <div key={`event-procedure-form`}>{form}</div>
                ))
              }
            </>
          ) : (
            <>
              <ControlLabel>
                Event Procedure Summary
              </ControlLabel>
              <p>This Event does not have any Event Procedures yet.</p>
              <Button bsStyle="success" onClick={() => setShowAddEventProcedureForm(true)}>
                Add Event Procedure
              </Button>
            </>
          )}
        </>
      );
    }

    return (
      <div style={{ width: '100%' }}>
        <ControlLabel>
          Remediation Steps <small className="text-muted">(Optional)</small>
        </ControlLabel>
        {readOnly ? (
          <MarkdownPreview
            show
            withFullView
            height={150}
            value={eventDefinition.remediation_steps || 'No remediation steps given'}
          />
        ) : (
          <MarkdownEditor
            id="event-definition-remediation-steps"
            readOnly={readOnly}
            height={150}
            value={eventDefinition.remediation_steps}
            onChange={(newValue: string) =>
              handleChange({ target: { name: 'remediation_steps', value: newValue } })
            }
          />
        )}
      </div>
    );
  };

  return (
    <Row>
      <Col md={7} lg={12}>
        <h2 className={commonStyles.title}>Event Details</h2>
        <fieldset>
          <div style={{ display: 'flex', flexDirection: 'row', gap: '2rem' }}>
            <Input
              id="event-definition-title"
              name="title"
              label="Title"
              type="text"
              bsStyle={validation.errors.title ? 'error' : null}
              help={get(
                validation,
                'errors.title[0]',
                'Title for this Event Definition, Events and Alerts created from it.',
              )}
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
          </div>

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

          {renderEventProcedure()}

        </fieldset>
      </Col>
    </Row>
  );
};

export default EventDetailsForm;
