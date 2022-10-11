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
import React, { useEffect } from 'react';

import { Row, Col } from 'components/bootstrap';
import CreateStreamButton from 'components/streams/CreateStreamButton';
import StreamComponent from 'components/streams/StreamComponent';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, IfPermitted, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import StreamsStore from 'stores/streams/StreamsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { useStore } from 'stores/connect';

const StreamsPage = () => {
  const currentUser = useCurrentUser();
  const { indexSets } = useStore(IndexSetsStore);

  useEffect(() => {
    IndexSetsActions.list(false);
  });

  const isLoading = !currentUser || !indexSets;

  const onSave = (_, stream) => {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  };

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Streams">
      <div>
        <PageHeader title="Streams"
                    subactions={(
                      <IfPermitted permissions="streams:create">
                        <CreateStreamButton bsStyle="success"
                                            onSave={onSave}
                                            indexSets={indexSets} />
                      </IfPermitted>
          )}>
          <span>
            You can route incoming messages into streams by applying rules against them. Messages matching
            the rules of a stream are routed into it. A message can also be routed into multiple streams.
          </span>

          <span>
            Read more about streams in the <DocumentationLink page={DocsHelper.PAGES.STREAMS} text="documentation" />.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <StreamComponent currentUser={currentUser}
                             onStreamSave={onSave}
                             indexSets={indexSets} />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default StreamsPage;
