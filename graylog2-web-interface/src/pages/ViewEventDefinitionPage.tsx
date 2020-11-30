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
// @flow strict
import * as React from 'react';
import { useState, useEffect, useContext } from 'react';

import { useStore } from 'stores/connect';
import withParams from 'routing/withParams';
import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import CurrentUserContext from 'contexts/CurrentUserContext';
import DocumentationLink from 'components/support/DocumentationLink';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';
import EventDefinitionSummary from 'components/event-definitions/event-definition-form/EventDefinitionSummary';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

type Props = {
  params: {
    definitionId: string,
  },
};

const ViewEventDefinitionPage = ({ params }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const [eventDefinition, setEventDefinition] = useState<{ title: string } | undefined>();
  const { all: notifications } = useStore(EventNotificationsStore);

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
  }, [currentUser, params]);

  if (!eventDefinition || !notifications) {
    return (
      <DocumentTitle title="View Event Definition">
        <span>
          <PageHeader title="View Event Definition">
            <Spinner text="Loading Event Definition..." />
            <></>
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={`View "${eventDefinition.title}" Event Definition`}>
      <span>
        <PageHeader title={`View "${eventDefinition.title}" Event Definition`}>
          <span>
            Event Definitions allow you to create Events from different Conditions and alert on them.
          </span>

          <span>
            Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                               text="documentation" />
          </span>

          <ButtonToolbar>
            <LinkContainer to={Routes.ALERTS.LIST}>
              <Button bsStyle="info">Alerts & Events</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
              <Button bsStyle="info">Event Definitions</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
              <Button bsStyle="info">Notifications</Button>
            </LinkContainer>
          </ButtonToolbar>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventDefinitionSummary eventDefinition={eventDefinition}
                                    currentUser={currentUser}
                                    notifications={notifications} />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default withParams(ViewEventDefinitionPage);
