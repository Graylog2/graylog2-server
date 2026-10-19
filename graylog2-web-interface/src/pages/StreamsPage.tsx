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
import React, { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import StreamsOverview from 'components/streams/StreamsOverview';
import PageHeader from 'components/common/PageHeader';
import { DocumentTitle, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import type { Stream } from 'logic/streams/types';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { EntityShare } from 'actions/permissions/EntityShareActions';
import useStreamMutations from 'hooks/useStreamMutations';
import CreateButton from 'components/common/CreateButton';
import Routes from 'routing/Routes';
import useLocation from 'routing/useLocation';
import StreamModal from 'components/streams/StreamModal';
import useHistory from 'routing/useHistory';

const StreamsPage = () => {
  const { t } = useTranslation('streams');
  const {
    data: { indexSets },
    isInitialLoading: isLoadingIndexSets,
  } = useIndexSetsList(false);
  const sendTelemetry = useSendTelemetry();
  const { createStream } = useStreamMutations();
  const { pathname } = useLocation();
  const showCreateModal = pathname === Routes.STREAM_NEW;
  const history = useHistory();
  const closeCreateModal = useCallback(() => history.push(Routes.STREAMS), [history]);

  const onCreate = async (stream: Stream & EntityShare) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.NEW_STREAM_CREATED, {
      app_pathname: 'streams',
    });

    return createStream(stream).then(() => {
      UserNotification.success(t('streams_page.create_success_message'), t('streams_page.create_success_title'));
    });
  };

  const isLoading = isLoadingIndexSets;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('streams_page.title')}>
      <PageHeader
        title={t('streams_page.title')}
        documentationLink={{
          title: t('streams_page.documentation_link_title'),
          path: DocsHelper.PAGES.STREAMS,
        }}
        actions={<CreateButton entityKey="Stream" />}>
        <span>{t('streams_page.description')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <StreamsOverview indexSets={indexSets} />
        </Col>
      </Row>
      {showCreateModal && (
        <StreamModal
          title={t('streams_page.create_title')}
          submitButtonText={t('streams_page.create_submit')}
          submitLoadingText={t('streams_page.create_loading')}
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
