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
import { useNavigate, useParams } from 'react-router-dom';

import { useStore } from 'stores/connect';
import { LinkContainer } from 'components/common/router';
import { ButtonToolbar, Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, IfPermitted, PageHeader, Spinner, ConfirmDialog } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import EventDefinitionSummary from 'components/event-definitions/event-definition-form/EventDefinitionSummary';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import { EventNotificationsActions, EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const ViewEventDefinitionPage = () => {
  const params = useParams<{ definitionId?: string }>();
  const currentUser = useCurrentUser();
  const [eventDefinition, setEventDefinition] = useState<EventDefinition | undefined>();
  const [showDialog, setShowDialog] = useState(false);
  const { all: notifications } = useStore(EventNotificationsStore);
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const navigate = useNavigate();

  const isSystemEventDefinition = (): boolean => eventDefinition?.config?.type === 'system-notifications-v1';

  useEffect(() => {
    if (currentUser && isPermitted(currentUser.permissions, `eventdefinitions:read:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (response) => {
            const eventDefinitionResp = response.event_definition;

            // Inject an internal "_is_scheduled" field to indicate if the event definition should be scheduled in the
            // backend. This field will be removed in the event definitions store before sending an event definition
            // back to the server.
            eventDefinitionResp.config._is_scheduled = response.context.scheduler.is_scheduled;
            setEventDefinition(eventDefinitionResp);
          },
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );

      EventNotificationsActions.listAll();
    }
  }, [currentUser, history, params]);

  const handleDuplicateEvent = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DUPLICATED, {
      app_pathname: 'event-definition',
    });

    EventDefinitionsActions.copy(eventDefinition).then((duplicatedEvent) => {
      navigate(Routes.ALERTS.DEFINITIONS.edit(duplicatedEvent.id));
    });
  };

  if (!eventDefinition || !notifications) {
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
        <PageHeader title={`View "${eventDefinition.title}" Event Definition`}
                    actions={(
                      <ButtonToolbar>
                        <IfPermitted permissions={`eventdefinitions:edit:${params.definitionId}`}>
                          <LinkContainer to={Routes.ALERTS.DEFINITIONS.edit(params.definitionId)}>
                            <Button bsStyle="success">Edit Event Definition</Button>
                          </LinkContainer>
                        </IfPermitted>
                        {!isSystemEventDefinition() && (
                          <IfPermitted permissions="eventdefinitions:create">
                            <Button onClick={() => setShowDialog(true)} bsStyle="success">Duplicate Event
                              Definition
                            </Button>
                          </IfPermitted>
                        )}
                      </ButtonToolbar>
                  )}
                    documentationLink={{
                      title: 'Alerts documentation',
                      path: DocsHelper.PAGES.ALERTS,
                    }}>
          <span>
            Event Definitions allow you to create Events from different Conditions and alert on them.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventDefinitionSummary eventDefinition={eventDefinition}
                                    currentUser={currentUser}
                                    notifications={notifications} />
          </Col>
        </Row>
      </DocumentTitle>
      {showDialog && (
      <ConfirmDialog title="Copy Event Definition"
                     show
                     onConfirm={() => handleDuplicateEvent()}
                     onCancel={() => setShowDialog(false)}>
        {`Are you sure you want to create a copy of "${eventDefinition.title}"?`}
      </ConfirmDialog>
      )}
    </>
  );
};

export default ViewEventDefinitionPage;
