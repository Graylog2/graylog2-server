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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import connect from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import EventsPageNavigation from 'components/events/EventsPageNavigation';

class CreateEventDefinitionPage extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinitionTitle: undefined,
    };
  }

  handleEventDefinitionChange = (eventDefinition) => {
    const { eventDefinitionTitle } = this.state;

    if (eventDefinition.title !== eventDefinitionTitle) {
      this.setState({ eventDefinitionTitle: eventDefinition.title });
    }
  };

  render() {
    const { eventDefinitionTitle } = this.state;
    const pageTitle = eventDefinitionTitle ? `New Event Definition "${eventDefinitionTitle}"` : 'New Event Definition';

    const { currentUser } = this.props;

    if (!isPermitted(currentUser.permissions, 'eventdefinitions:create')) {
      history.push(Routes.NOTFOUND);
    }

    return (
      <DocumentTitle title={pageTitle}>
        <EventsPageNavigation />

        <PageHeader title={pageTitle}>
          <span>
            Event Definitions allow you to create Alerts from different Conditions and alert on them.
          </span>

          <span>
            Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                               text="documentation" />
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventDefinitionFormContainer action="create"
                                          onEventDefinitionChange={this.handleEventDefinitionChange} />
          </Col>
        </Row>

      </DocumentTitle>
    );
  }
}

export default connect(CreateEventDefinitionPage, {
  currentUser: CurrentUserStore,
}, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));
