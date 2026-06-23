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
import { useState, useEffect } from 'react';
import upperFirst from 'lodash/upperFirst';

import { Col, Row } from 'components/bootstrap';
import { TagList } from 'components/common';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import type User from 'logic/users/User';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import EventDefinitionValidationSummary from './EventDefinitionValidationSummary';
import styles from './EventDefinitionSummary.css';
import ShareDetails from './ShareDetails';
import {
  useEventProcedureSummaryComponents,
  renderCondition,
  renderFields,
  renderNotifications,
  useTechniquesSummary,
} from './useEventDefinitionSummaryRenders';

import type { EventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';
import { SYSTEM_EVENT_DEFINITION_TYPE } from '../constants';

type Props = {
  eventDefinition: EventDefinition & {
    share_request?: EntitySharePayload;
  };
  notifications: Array<EventNotification>;
  validation?: {
    errors: {
      title?: string;
    };
  };
  currentUser: User;
};

const EventDefinitionSummary = ({
  eventDefinition,
  notifications,
  validation = { errors: {} },
  currentUser,
}: Props) => {
  const [showValidation, setShowValidation] = useState<boolean>(false);
  const { label: eventProcedureSummaryLabel, Component: EventProcedureSummaryComponent } =
    useEventProcedureSummaryComponents({
      eventProcedureId: eventDefinition?.event_procedure,
      remediationSteps: eventDefinition.remediation_steps,
    });

  const techniquesSummaryComponent = useTechniquesSummary(eventDefinition);

  useEffect(() => {
    const flipShowValidation = () => {
      if (!showValidation) {
        setShowValidation(true);
      }
    };

    flipShowValidation();
  }, [showValidation, setShowValidation]);

  const renderDetails = () => (
    <>
      <h3 className={commonStyles.title}>Details</h3>
      <dl>
        <dt>Title</dt>
        <dd>{eventDefinition.title || 'No title given'}</dd>
        <dt>Description</dt>
        <dd>{eventDefinition.description || 'No description given'}</dd>
        <dt>Priority</dt>
        <dd>
          {upperFirst(
            EventDefinitionPriorityEnum.properties[
              eventDefinition.priority as keyof typeof EventDefinitionPriorityEnum.properties
            ].name,
          )}
        </dd>
        <dt>Tags</dt>
        <dd>
          <TagList tags={eventDefinition.tags} emptyFallback={<em>No tags</em>} />
        </dd>
        {techniquesSummaryComponent}
        {eventDefinition.event_summary_template && (
          <>
            <dt>Event Summary Template</dt>
            <dd>{eventDefinition.event_summary_template}</dd>
          </>
        )}
        {eventProcedureSummaryLabel && EventProcedureSummaryComponent && (
          <>
            <dt style={{ margin: '16px 0 0' }}>{eventProcedureSummaryLabel}</dt>
            <dd>{EventProcedureSummaryComponent}</dd>
          </>
        )}
      </dl>
    </>
  );

  const isSystemEventDefinition = eventDefinition.config.type === SYSTEM_EVENT_DEFINITION_TYPE;

  return (
    <Row className={styles.eventSummary}>
      <Col md={12}>
        <h2 className={commonStyles.title}>Event Summary</h2>
        {showValidation && <EventDefinitionValidationSummary validation={validation} />}
        <Row>
          <Col md={5}>{renderDetails()}</Col>

          {!isSystemEventDefinition && (
            <Col md={5} mdOffset={1}>
              {renderCondition(eventDefinition, eventDefinition.id, currentUser)}
            </Col>
          )}
        </Row>
        <Row>
          {!isSystemEventDefinition && (
            <Col md={5}>{renderFields(eventDefinition.field_spec, eventDefinition.key_spec, currentUser)}</Col>
          )}
          <Col md={5} mdOffset={isSystemEventDefinition ? 0 : 1}>
            {renderNotifications(
              eventDefinition.notifications,
              eventDefinition.notification_settings,
              notifications,
              currentUser,
            )}
          </Col>
        </Row>
        <Row>
          <Col md={5}>
            <ShareDetails shareState={eventDefinition.share_request} entityId={eventDefinition.id} />
          </Col>
        </Row>
      </Col>
    </Row>
  );
};

export default EventDefinitionSummary;
