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
import { useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { TagList } from 'components/common';
import type User from 'logic/users/User';
import usePermissions from 'hooks/usePermissions';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';

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
import { priorityTitle } from '../pluginTitles';

type Props = {
  eventDefinition: EventDefinition;
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
  const { t } = useTranslation('event_definitions');
  const { isPermitted } = usePermissions();
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
      <h3 className={commonStyles.title}>{t('definition_summary.details')}</h3>
      <dl>
        <dt>{t('details_form.title_label')}</dt>
        <dd>{eventDefinition.title || t('definition_summary.no_title')}</dd>
        <dt>{t('details_form.description')}</dt>
        <dd>{eventDefinition.description || t('definition_summary.no_description')}</dd>
        <dt>{t('details_form.priority')}</dt>
        <dd>{priorityTitle(eventDefinition.priority)}</dd>
        <dt>{t('fields_form.tags')}</dt>
        <dd>
          <TagList tags={eventDefinition.tags} emptyFallback={<em>{t('definition_summary.no_tags')}</em>} />
        </dd>
        {techniquesSummaryComponent}
        {eventDefinition.event_summary_template && (
          <>
            <dt>{t('details_form.summary_template')}</dt>
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
        <h2 className={commonStyles.title}>{t('definition_summary.title')}</h2>
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
              isPermitted,
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
