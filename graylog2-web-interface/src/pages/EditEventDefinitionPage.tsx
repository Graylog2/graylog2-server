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

import useScopePermissions from 'hooks/useScopePermissions';
import { LinkContainer } from 'components/common/router';
import { ButtonToolbar, Col, Row, Button } from 'components/bootstrap';
import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';
import connect from 'stores/connect';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';
import withParams from 'routing/withParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import type { UserJSON } from 'logic/users/User';
import type { EventDefinition } from 'logic/alerts/types';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

type Props = {
  params: {
    definitionId: string,
  },
  currentUser: UserJSON,
};

const EditEventDefinitionPage = ({ params, currentUser }: Props) => {
  const [eventDefinition, setEventDefinition] = React.useState<EventDefinition>(undefined);

  React.useEffect(() => {
    if (isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (response) => {
            const eventDefinitionResponse = response.event_definition;

            // Inject an internal "_is_scheduled" field to indicate if the event definition should be scheduled in the
            // backend. This field will be removed in the event definitions store before sending an event definition
            // back to the server.
            eventDefinitionResponse.config._is_scheduled = response.context.scheduler.is_scheduled;
            setEventDefinition(eventDefinitionResponse);
          },
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );
    }
  }, [params, currentUser]);

  const { loadingScopePermissions, scopePermissions } = useScopePermissions(eventDefinition);

  const streamsWithMissingPermissions = () => {
    const streams = eventDefinition?.config?.streams || [];

    return streams.filter((streamId) => !isPermitted(currentUser.permissions, `streams:read:${streamId}`));
  };

  if (!isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
    history.push(Routes.NOTFOUND);
  }

  const missingStreams = streamsWithMissingPermissions();

  if (missingStreams.length > 0) {
    return <StreamPermissionErrorPage error={null} missingStreamIds={missingStreams} />;
  }

  if (!eventDefinition || loadingScopePermissions) {
    return (
      <DocumentTitle title="Edit Event Definition">
        <span>
          <PageHeader title="Edit Event Definition">
            <Spinner text="Loading Event Definition..." />
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={`Edit "${eventDefinition.title}" Event Definition`}>
      <span>
        <PageHeader title={`Edit "${eventDefinition.title}" Event Definition`}>
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
            <IfPermitted permissions="eventdefinitions:read">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Event Definitions</Button>
              </LinkContainer>
            </IfPermitted>
            <IfPermitted permissions="eventnotifications:read">
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
                <Button bsStyle="info">Notifications</Button>
              </LinkContainer>
            </IfPermitted>
          </ButtonToolbar>
        </PageHeader>
        {scopePermissions.is_mutable ? (
          <Row className="content">
            <Col md={12}>
              <EventDefinitionFormContainer action="edit" eventDefinition={eventDefinition} />
            </Col>
          </Row>
        ) : (
          <Row className="content">
            <Col md={12}>
              <Row>
                <Col md={6} mdOffset={3} lg={4} lgOffset={4}>
                  <div style={{ textAlign: 'center' }}>
                    <p>This particular Event Definition has been marked as immutable when it was created, therefore it cannot be edited.</p>
                  </div>
                </Col>
              </Row>
            </Col>
          </Row>
        )}
      </span>
    </DocumentTitle>
  );
};

export default connect(withParams(EditEventDefinitionPage), {
  currentUser: CurrentUserStore,
}, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));
