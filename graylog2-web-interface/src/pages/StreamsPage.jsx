import React from 'react';
import { Row, Col } from 'react-bootstrap';

import connect from 'stores/connect';

import CreateStreamButton from 'components/streams/CreateStreamButton';
import StreamComponent from 'components/streams/StreamComponent';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, IfPermitted } from 'components/common';

import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');
const IndexSetsStore = StoreProvider.getStore('IndexSets');

import ActionsProvider from 'injection/ActionsProvider';
const IndexSetsActions = ActionsProvider.getActions('IndexSets');

class StreamsPage extends React.Component {
  state = {};
  _onSave(_, stream) {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  }
  render() {
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
              <CreateStreamButton bsSize="large"
                                  bsStyle="success"
                                  onSave={this._onSave}
                                  indexSets={this.props.indexSets.indexSets} />
            </IfPermitted>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <StreamComponent currentUser={this.props.currentUser.currentUser}
                               onStreamSave={this._onSave}
                               indexSets={this.props.indexSets.indexSets} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  }
}

export default connect(StreamsPage, { currentUser: CurrentUserStore, indexSets: IndexSetsStore }, [() => IndexSetsActions.list(false)]);
