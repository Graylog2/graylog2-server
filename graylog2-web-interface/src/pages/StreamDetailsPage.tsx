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
import * as React from 'react';
import { useParams } from 'react-router-dom';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useStream from 'hooks/useStream';
import { Col, Row } from 'components/bootstrap';
import StreamDataRoutingInstake from 'components/streams/StreamDetails/StreamDataRoutingIntake';
import StreamDataRoutingProcessing from 'components/streams/StreamDetails/StreamDataRoutingProcessing';

const StreamDetailsPage = () => {
  const { streamId } = useParams<{ streamId: string }>();
  const { data: stream, isInitialLoading } = useStream(streamId);

  if (isInitialLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Stream Details Page">
      <PageHeader title={`Stream: ${stream.title} `}
                  documentationLink={{
                    title: 'Streams documentation',
                    path: DocsHelper.PAGES.STREAMS,
                  }}>
        <p>{stream.description}</p>
      </PageHeader>
      <Row className="content">
        <Col xs={12}>
          <StreamDataRoutingInstake stream={stream} />
          <StreamDataRoutingProcessing />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default StreamDetailsPage;
