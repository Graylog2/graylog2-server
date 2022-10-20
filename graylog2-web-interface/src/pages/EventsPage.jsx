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
import DocumentationLink from 'components/support/DocumentationLink';
import EventsContainer from 'components/events/events/EventsContainer';
import DocsHelper from 'util/DocsHelper';
import withLocation from 'routing/withLocation';
import EventsPageNavigation from 'components/events/EventsPageNavigation';

const EventsPage = ({ location }) => {
  const filteredSourceStream = location.query.stream_id;

  return (
    <DocumentTitle title="Alerts &amp; Events">
      <EventsPageNavigation />
      <PageHeader title="Alerts &amp; Events">
        <span>
          Define Events through different conditions. Add Notifications to Events that require your attention
          to create Alerts.
        </span>
        <span>
          Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                             text="documentation" />
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <EventsContainer key={filteredSourceStream} streamId={filteredSourceStream} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

EventsPage.propTypes = {
  location: PropTypes.object.isRequired,
};

export default withLocation(EventsPage);
