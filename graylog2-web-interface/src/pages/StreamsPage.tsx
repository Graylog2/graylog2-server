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
import StreamsOverview from 'components/streams/StreamsOverview';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, IfPermitted, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { useStore } from 'stores/connect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const StreamsPage = () => {
  const { indexSets } = useStore(IndexSetsStore);
  const sendTelemetry = useSendTelemetry();

  const onSave = (stream: Stream) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.NEW_STREAM_CREATED, {
      app_pathname: 'streams',
    });

    return StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  };

  useEffect(() => {
    IndexSetsActions.list(false);
  }, []);

  const isLoading = !indexSets;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Streams">
      <PageHeader title="Streams"
                  documentationLink={{
                    title: 'Streams documentation',
                    path: DocsHelper.PAGES.STREAMS,
                  }}
                  actions={(
                    <IfPermitted permissions="streams:create">
                      <CreateStreamButton bsStyle="success"
                                          onCreate={onSave}
                                          indexSets={indexSets} />
                    </IfPermitted>
                  )}>
        <span>
          You can route incoming messages into streams by applying rules against them. Messages matching
          the rules of a stream are routed into it. A message can also be routed into multiple streams.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <StreamsOverview indexSets={indexSets} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default StreamsPage;
