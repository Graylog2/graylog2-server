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
import React, { useCallback, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Row, Col } from 'components/bootstrap';
import StreamsOverview from 'components/streams/StreamsOverview';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import type { Stream } from 'stores/streams/StreamsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { useStore } from 'stores/connect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { EntityShare } from 'actions/permissions/EntityShareActions';
import useStreamMutations from 'hooks/useStreamMutations';
import { KEY_PREFIX } from 'components/streams/hooks/useStreams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import CreateButton from 'components/common/CreateButton';
import Routes from 'routing/Routes';
import useLocation from 'routing/useLocation';
import StreamModal from 'components/streams/StreamModal';
import useHistory from 'routing/useHistory';

const StreamsPage = () => {
  const { indexSets } = useStore(IndexSetsStore);
  const sendTelemetry = useSendTelemetry();
  const { createStream } = useStreamMutations();
  const queryClient = useQueryClient();
  const { pathname } = useLocation();
  const [showCreateModal, setShowCreateModal] = useState(false);
  useEffect(() => {
    setShowCreateModal(pathname === Routes.STREAM_NEW);
  }, [pathname]);
  const history = useHistory();
  const closeCreateModal = useCallback(() => history.push(Routes.STREAMS), [history]);

  const onCreate = async (stream: Stream & EntityShare) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.NEW_STREAM_CREATED, {
      app_pathname: 'streams',
    });

    return createStream(stream).then(() => {
      UserNotification.success('Stream has been successfully created.', 'Success');
      queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
      CurrentUserStore.reload();
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
      <PageHeader
        title="Streams"
        documentationLink={{
          title: 'Streams documentation',
          path: DocsHelper.PAGES.STREAMS,
        }}
        actions={<CreateButton entityKey="Stream" />}>
        <span>
          You can route incoming messages into streams by applying rules against them. Messages matching the rules of a
          stream are routed into it. A message can also be routed into multiple streams.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <StreamsOverview indexSets={indexSets} />
        </Col>
      </Row>
      {showCreateModal && (
        <StreamModal
          title="Create stream"
          submitButtonText="Create stream"
          submitLoadingText="Creating stream..."
          indexSets={indexSets}
          onSubmit={onCreate}
          onClose={closeCreateModal}
          isNew
        />
      )}
    </DocumentTitle>
  );
};

export default StreamsPage;
