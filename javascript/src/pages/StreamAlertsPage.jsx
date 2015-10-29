import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';

import StreamsStore from 'stores/streams/StreamsStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import Spinner from 'components/common/Spinner';

import AlarmCallbackComponent from 'components/alarmcallbacks/AlarmCallbackComponent';
import AlertsComponent from 'components/alerts/AlertsComponent';

const StreamAlertsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  getInitialState() {
    return {
      stream: undefined,
    };
  },
  componentDidMount() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({stream: stream});
    });
  },
  render() {
    if (!this.state.stream) {
      return <Spinner />;
    }
    const stream = this.state.stream;
    return (
      <span>
        <PageHeader title={'Alerts configuration for stream »' + stream.title + '«'}>
          <span>You can define thresholds on any message field or message count of a stream and be alerted based on this definition.</span>
          <span>
            Learn more about alerts in the <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation"/>.</span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <h2>Callbacks</h2>
            <p className="description">
              The following callbacks will be performed when this stream triggers an alert.
            </p>

            <AlarmCallbackComponent streamId={stream.id} permissions={this.state.currentUser.permissions} />
          </Col>
        </Row>

        <AlertsComponent streamId={stream.id} />
      </span>
    );
  }
});

export default StreamAlertsPage;
