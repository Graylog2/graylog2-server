import React from 'react';
import PropTypes from 'prop-types';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { StreamAlertNotifications } from 'components/alertnotifications';
import { StreamAlertConditions } from 'components/alertconditions';
import { StreamAlerts } from 'components/alerts';

const { StreamsStore } = CombinedProvider.get('Streams');

class StreamAlertsOverviewPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    stream: undefined,
  };

  componentDidMount() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({ stream: stream });
    });
  }

  render() {
    const { stream } = this.state;
    const isLoading = !stream;

    if (isLoading) {
      return <DocumentTitle title="Stream Alerts Overview"><Spinner /></DocumentTitle>;
    }

    return (
      <DocumentTitle title={`Alerts for Stream ${stream.title}`}>
        <div>
          <PageHeader title={<span>Alerts for Stream &raquo;{stream.title}&laquo;</span>}>
            <span>
              Notifications let you be aware of changes in your alert conditions status any time. Graylog can send
              notifications directly to you or to other systems you use for that purpose.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.STREAMS}>
                <Button bsStyle="info">Streams</Button>
              </LinkContainer>
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info">Alerts</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <StreamAlerts stream={stream} />
            </Col>
          </Row>

          <Row className="content">
            <Col md={12}>
              <StreamAlertConditions stream={stream} />
            </Col>
          </Row>

          <Row className="content">
            <Col md={12}>
              <StreamAlertNotifications stream={stream} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  }
}

export default StreamAlertsOverviewPage;
