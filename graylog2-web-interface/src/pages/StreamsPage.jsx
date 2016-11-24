import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import CreateStreamButton from 'components/streams/CreateStreamButton';
import StreamComponent from 'components/streams/StreamComponent';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';
import { IfPermitted, Spinner } from 'components/common';

import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');
const IndexSetsStore = StoreProvider.getStore('IndexSets');

import ActionsProvider from 'injection/ActionsProvider';
const IndexSetsActions = ActionsProvider.getActions('IndexSets');

const StreamsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(IndexSetsStore)],
  getInitialState() {
    return {
      indexSets: [],
    };
  },
  componentDidMount() {
    IndexSetsActions.list();
  },
  _isLoading() {
    return !this.state.currentUser;
  },
  _onSave(_, stream) {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <PageHeader title="Streams">
          <span>You can route incoming messages into streams by applying rules against them. If a
            message
            matches all rules of a stream it is routed into it. A message can be routed into
            multiple
            streams. You can for example create a stream that contains all SSH logins and configure
            to be alerted whenever there are more logins than usual.

            Read more about streams in the <DocumentationLink page={DocsHelper.PAGES.STREAMS} text="documentation"/>.</span>

          <span>
            Take a look at the
            {' '}<DocumentationLink page={DocsHelper.PAGES.EXTERNAL_DASHBOARDS} text="Graylog stream dashboards"/>{' '}
            for wall-mounted displays or other integrations.
          </span>

          <IfPermitted permissions="streams:create">
            <CreateStreamButton ref="createStreamButton" bsSize="large" bsStyle="success" onSave={this._onSave} indexSets={this.state.indexSets} />
          </IfPermitted>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <StreamComponent currentUser={this.state.currentUser} onStreamSave={this._onSave} indexSets={this.state.indexSets}/>
          </Col>
        </Row>
      </div>
    );
  },
});

export default StreamsPage;
