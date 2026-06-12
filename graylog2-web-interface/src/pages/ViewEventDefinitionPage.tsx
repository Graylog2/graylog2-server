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
import { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

import { useStore } from 'stores/connect';
import { ButtonToolbar, Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, IfPermitted, PageHeader, Spinner, ConfirmDialog } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import EventDefinitionSummary from 'components/event-definitions/event-definition-form/EventDefinitionSummary';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import { EventNotificationsActions, EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import { isSystemEventDefinition, isSigmaEventDefinition } from 'components/event-definitions/event-definitions-types';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import usePluginEntities from 'hooks/usePluginEntities';
import { useGetEventDefinition } from 'components/event-definitions/hooks/useEventDefinitions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';

import useEventDefinitionDetailSections from './useEventDefinitionDetailSections';

type SigmaEventDefinitionConfig = EventDefinition['config'] & {
  sigma_rule_id: string;
};

const ViewEventDefinitionPage = () => {
  const params = useParams<{ definitionId?: string }>();
  const currentUser = useCurrentUser();
  const [showDialog, setShowDialog] = useState(false);
  const { all: notifications } = useStore(EventNotificationsStore);
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const navigate = useNavigate();
  const [showSigmaModal, setShowSigmaModal] = useState(false);
  const detailSections = useEventDefinitionDetailSections();

  const pluggableSigmaModal = usePluginEntities('eventDefinitions.components.editSigmaModal').find(
    (entity: { key: string }) => entity.key === 'coreSigmaModal',
  );

  const CoreSigmaModal = pluggableSigmaModal
    ? (pluggableSigmaModal.component as React.FC<{ ruleId: string; onCancel: () => void; onConfirm: () => void }>)
    : null;

  const queryClient = useQueryClient();
  const { data, isFetching } = useGetEventDefinition(params.definitionId);

  const eventDefinition = useMemo(() => {
    if (!data?.eventDefinition) return null;

    return {
      ...data.eventDefinition,
      config: {
        ...data.eventDefinition.config,
        _is_scheduled: data.context?.scheduler?.is_scheduled,
      },
    };
  }, [data]);

  const { scopePermissions } = useGetPermissionsByScope(eventDefinition);

  useEffect(() => {
    EventNotificationsActions.listAll();
  }, []);

  useEffect(() => {
    if (!isFetching && !eventDefinition) {
      history.push(Routes.ALERTS.DEFINITIONS.LIST);
    }
  }, [eventDefinition, history, isFetching]);

  const handleDuplicateEvent = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DUPLICATED, {
      app_pathname: 'event-definition',
    });

    EventDefinitionsActions.copy(eventDefinition).then((duplicatedEvent: any) => {
      navigate(Routes.ALERTS.DEFINITIONS.edit(duplicatedEvent.id));
    });
  };

  const onEditEventDefinition = () => navigate(Routes.ALERTS.DEFINITIONS.edit(params.definitionId));

  const onSigmaModalClose = () => {
    queryClient.invalidateQueries({ queryKey: ['get-event-definition', params.definitionId] });
    setShowSigmaModal(false);
  };

  if (isFetching || !eventDefinition || !notifications) {
    return (
      <DocumentTitle title="View Event Definition">
        <span>
          <PageHeader title="View Event Definition">
            <Spinner text="Loading Event Definition..." />
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <>
      <DocumentTitle title={`View "${eventDefinition.title}" Event Definition`}>
        <EventsPageNavigation />
        <PageHeader
          title={`View "${eventDefinition.title}" Event Definition`}
          actions={
            <ButtonToolbar>
              {(!isSigmaEventDefinition(eventDefinition) || scopePermissions?.is_mutable) && (
                <IfPermitted permissions={`eventdefinitions:edit:${params.definitionId}`}>
                  <Button bsStyle="primary" onClick={onEditEventDefinition}>
                    Edit Event Definition
                  </Button>
                </IfPermitted>
              )}
              {!isSystemEventDefinition(eventDefinition) && !isSigmaEventDefinition(eventDefinition) && (
                <IfPermitted permissions="eventdefinitions:create">
                  <Button onClick={() => setShowDialog(true)}>Duplicate Event Definition</Button>
                </IfPermitted>
              )}
            </ButtonToolbar>
          }
          documentationLink={{
            title: 'Alerts documentation',
            path: DocsHelper.PAGES.ALERTS,
          }}>
          <span>Event Definitions allow you to create Events from different Conditions and alert on them.</span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {detailSections.map(({ key, component: Component }) => (
              <Component key={key} eventDefinition={eventDefinition} />
            ))}
            <EventDefinitionSummary
              eventDefinition={eventDefinition}
              currentUser={currentUser}
              notifications={notifications}
            />
          </Col>
        </Row>
      </DocumentTitle>
      {showDialog && (
        <ConfirmDialog
          title="Copy Event Definition"
          show
          onConfirm={() => handleDuplicateEvent()}
          onCancel={() => setShowDialog(false)}>
          {`Are you sure you want to create a copy of "${eventDefinition.title}"?`}
        </ConfirmDialog>
      )}
      {showSigmaModal && CoreSigmaModal && (
        <CoreSigmaModal
          ruleId={(eventDefinition.config as SigmaEventDefinitionConfig).sigma_rule_id}
          onCancel={onSigmaModalClose}
          onConfirm={onSigmaModalClose}
        />
      )}
    </>
  );
};

export default ViewEventDefinitionPage;
