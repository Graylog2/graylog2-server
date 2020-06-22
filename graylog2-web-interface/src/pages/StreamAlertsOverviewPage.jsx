import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { StreamAlertsOverviewContainer } from 'components/alerts';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

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
      <DocumentTitle title={`Alerting overview for Stream ${stream.title}`}>
        <div>
          <PageHeader title={<span>Alerting overview for Stream &raquo;{stream.title}&laquo;</span>}>
            <span>
              This is an overview of alerts, conditions, and notifications belonging to the Stream{' '}
              <em>{stream.title}</em>.
            </span>

            <span>
              Read more about alerting in the <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.STREAMS}>
                <Button bsStyle="info">Streams</Button>
              </LinkContainer>
              <LinkContainer to={Routes.LEGACY_ALERTS.LIST}>
                <Button bsStyle="info">All Alerts</Button>
              </LinkContainer>
              <LinkContainer to={Routes.LEGACY_ALERTS.CONDITIONS}>
                <Button bsStyle="info">All Conditions</Button>
              </LinkContainer>
              <LinkContainer to={Routes.LEGACY_ALERTS.NOTIFICATIONS}>
                <Button bsStyle="info">All Notifications</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <StreamAlertsOverviewContainer stream={stream} />
        </div>
      </DocumentTitle>
    );
  }
}

export default StreamAlertsOverviewPage;
