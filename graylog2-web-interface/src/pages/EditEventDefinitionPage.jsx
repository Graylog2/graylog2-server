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
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';
import withParams from 'routing/withParams';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinition: undefined,
    };
  }

  componentDidMount() {
    const { params, currentUser } = this.props;

    if (isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (response) => {
            const eventDefinition = response.event_definition;

            // Inject an internal "_is_scheduled" field to indicate if the event definition should be scheduled in the
            // backend. This field will be removed in the event definitions store before sending an event definition
            // back to the server.
            eventDefinition.config._is_scheduled = response.context.scheduler.is_scheduled;
            this.setState({ eventDefinition });
          },
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );
    }
  }

  _streamsWithMissingPermissions(eventDefinition, currentUser) {
    const streams = eventDefinition?.config?.streams || [];

    return streams.filter((streamId) => !isPermitted(currentUser.permissions, `streams:read:${streamId}`));
  }

  render() {
    const { params, currentUser } = this.props;
    const { eventDefinition } = this.state;

    if (!isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      history.push(Routes.NOTFOUND);
    }

    const missingStreams = this._streamsWithMissingPermissions(eventDefinition, currentUser);

    if (missingStreams.length > 0) {
      return <StreamPermissionErrorPage error={{}} missingStreamIds={missingStreams} />;
    }

    if (!eventDefinition) {
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

          <Row className="content">
            <Col md={12}>
              <EventDefinitionFormContainer action="edit" eventDefinition={eventDefinition} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default connect(withParams(EditEventDefinitionPage), {
  currentUser: CurrentUserStore,
},
({ currentUser }) => ({ currentUser: currentUser.currentUser }));
