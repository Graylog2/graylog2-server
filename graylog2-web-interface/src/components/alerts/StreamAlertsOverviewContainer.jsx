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
import Promise from 'bluebird';

import { Col, Row } from 'components/graylog';
import { Spinner } from 'components/common';
import { StreamAlerts } from 'components/alerts';
import { StreamAlertConditions } from 'components/alertconditions';
import { StreamAlertNotifications } from 'components/alertnotifications';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const StreamAlertsOverviewContainer = createReactClass({
  propTypes: {
    stream: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      loading: true,
    };
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    this.setState({ loading: true });
    const promises = this.fetchData();

    Promise.all(promises).finally(() => this.setState({ loading: false }));
  },

  fetchData() {
    return [
      AlertConditionsActions.list(this.props.stream.id),
      AlertConditionsActions.available(),
    ];
  },

  render() {
    if (this.state.loading) {
      return <Spinner />;
    }

    const { stream } = this.props;
    const { alertConditions, availableConditions } = this.state;

    return (
      <>
        <Row className="content">
          <Col md={12}>
            <StreamAlerts stream={stream} alertConditions={alertConditions} availableConditions={availableConditions} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <StreamAlertConditions stream={stream}
                                   alertConditions={alertConditions}
                                   availableConditions={availableConditions}
                                   onConditionUpdate={this.fetchData}
                                   onConditionDelete={this.fetchData} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <StreamAlertNotifications stream={stream} />
          </Col>
        </Row>
      </>
    );
  },
});

export default StreamAlertsOverviewContainer;
