import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import { AlarmCallbackComponent } from 'components/alarmcallbacks';

const StreamAlertsPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],
  getInitialState() {
    return {
      stream: undefined,
    };
  },
  componentDidMount() {
    StreamsStore.onChange(this.loadData);
    this.loadData();
  },
  _onSendDummyAlert() {
    const stream = this.state.stream;
    StreamsStore.sendDummyAlert(stream.id).then(() => {
      UserNotification.success(`Sent dummy alert for stream »${stream.title}«`, 'Success!');
    }, (error) => {
      UserNotification.error(`Unable to send dummy alert for stream »${stream.title}«: ${error.message}`,
        'Sending dummy alert failed!');
    });
  },
  loadData() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({ stream: stream });
    });
  },
  render() {
    if (!this.state.stream) {
      return <Spinner />;
    }
    const stream = this.state.stream;
    return (
      <span>
        <PageHeader title={`Alerts configuration for stream »${stream.title}«`}>
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
      </span>
    );
  },
});

export default StreamAlertsPage;
