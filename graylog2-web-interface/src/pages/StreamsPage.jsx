import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import CreateStreamButton from 'components/streams/CreateStreamButton';
import StreamComponent from 'components/streams/StreamComponent';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, IfPermitted, Spinner } from 'components/common';

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
      indexSets: undefined,
    };
  },
  componentDidMount() {
    IndexSetsActions.list(false);
  },
  _isLoading() {
    return !this.state.currentUser || !this.state.indexSets;
  },
  _onSave(_, stream) {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Streams">
        <div>
          <PageHeader title="Streams">
            <span>
              You can route incoming messages into streams by applying rules against them. Messages matching
              the rules of a stream are routed into it. A message can also be routed into multiple streams.
            </span>

            <span>
              Read more about streams in the <DocumentationLink page={DocsHelper.PAGES.STREAMS} text="documentation" />.
            </span>

            <IfPermitted permissions="streams:create">
              <CreateStreamButton ref="createStreamButton" bsSize="large" bsStyle="success" onSave={this._onSave}
                                  indexSets={this.state.indexSets} />
            </IfPermitted>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <StreamComponent currentUser={this.state.currentUser} onStreamSave={this._onSave}
                               indexSets={this.state.indexSets} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default StreamsPage;
