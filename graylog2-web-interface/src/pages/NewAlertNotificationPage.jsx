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
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';

import { Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { CreateAlertNotificationInput } from 'components/alertnotifications';
import Routes from 'routing/Routes';
import StoreProvider from 'injection/StoreProvider';
import withLocation from 'routing/withLocation';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertNotificationPage = createReactClass({
  displayName: 'NewAlertNotificationPage',
  propTypes: {
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    const streamId = this.props.location.query.stream_id;

    return (
      <DocumentTitle title="New alert notification">
        <div>
          <PageHeader title="New alert notification">
            <span>
              Create a new notification that you can use to not miss any of your alerts.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.LEGACY_ALERTS.NOTIFICATIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertNotificationInput initialSelectedStream={streamId} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withLocation(NewAlertNotificationPage);
