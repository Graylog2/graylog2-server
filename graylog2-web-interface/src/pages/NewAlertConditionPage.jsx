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
import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { CreateAlertConditionInput } from 'components/alertconditions';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import StoreProvider from 'injection/StoreProvider';
import withLocation from 'routing/withLocation';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertConditionPage = createReactClass({
  displayName: 'NewAlertConditionPage',
  propTypes: {
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    const streamId = this.props.location.query.stream_id;

    return (
      <DocumentTitle title="New alert condition">
        <div>
          <PageHeader title="New alert condition">
            <span>
              Define an alert condition and configure the way Graylog will notify you when that condition is satisfied.
            </span>

            <span>
              Are the default conditions not flexible enough? You can write your own! Read more about alerting in
              the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.LEGACY_ALERTS.CONDITIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertConditionInput initialSelectedStream={streamId} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withLocation(NewAlertConditionPage);
