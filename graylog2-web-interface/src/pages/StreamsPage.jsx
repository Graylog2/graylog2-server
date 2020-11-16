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
import Reflux from 'reflux';

import { Row, Col } from 'components/graylog';
import CreateStreamButton from 'components/streams/CreateStreamButton';
import StreamComponent from 'components/streams/StreamComponent';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, IfPermitted, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');
const IndexSetsStore = StoreProvider.getStore('IndexSets');
const IndexSetsActions = ActionsProvider.getActions('IndexSets');

const StreamsPage = createReactClass({
  displayName: 'StreamsPage',
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
              <CreateStreamButton bsSize="large"
                                  bsStyle="success"
                                  onSave={this._onSave}
                                  indexSets={this.state.indexSets} />
            </IfPermitted>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <StreamComponent currentUser={this.state.currentUser}
                               onStreamSave={this._onSave}
                               indexSets={this.state.indexSets} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default StreamsPage;
