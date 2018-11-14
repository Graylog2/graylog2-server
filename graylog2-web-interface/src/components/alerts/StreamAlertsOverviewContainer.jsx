import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import Reflux from 'reflux';
import Promise from 'bluebird';

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
    const promises = [
      AlertConditionsActions.list(this.props.stream.id),
      AlertConditionsActions.available(),
    ];

    Promise.all(promises).finally(() => this.setState({ loading: false }));
  },

  render() {
    if (this.state.loading) {
      return <Spinner />;
    }

    const stream = this.props.stream;
    const { alertConditions, availableConditions } = this.state;

    return (
      <React.Fragment>
        <Row className="content">
          <Col md={12}>
            <StreamAlerts stream={stream} alertConditions={alertConditions} availableConditions={availableConditions} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <StreamAlertConditions stream={stream} alertConditions={alertConditions} availableConditions={availableConditions} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <StreamAlertNotifications stream={stream} />
          </Col>
        </Row>
      </React.Fragment>
    );
  },
});

export default StreamAlertsOverviewContainer;
