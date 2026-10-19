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
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';

import { Alert } from 'components/bootstrap';
import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import useQuery from 'routing/useQuery';
import DocsHelper from 'util/DocsHelper';
import useStream from 'components/streams/hooks/useStream';

const StreamEditPage = () => {
  const { t } = useTranslation('streams');
  const params = useParams<{ streamId: string }>();
  const query = useQuery() as { index: string; message_id: string };
  const { data: stream } = useStream(params.streamId);

  if (!stream) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('stream_edit_page.title', { title: stream.title })}>
      <div>
        <PageHeader
          title={<span>{t('stream_edit_page.title_span', { title: stream.title })}</span>}
          documentationLink={{
            title: t('stream_edit_page.documentation_link_title'),
            path: DocsHelper.PAGES.STREAMS,
          }}>
          <span>{t('stream_edit_page.description')}</span>
        </PageHeader>

        {!stream.is_default && (
          <StreamRulesEditor streamId={params.streamId} messageId={query.message_id} index={query.index} />
        )}

        {stream.is_default && (
          <div className="row content">
            <div className="col-md-12">
              <Alert bsStyle="danger">{t('stream_edit_page.default_stream_error')}</Alert>
            </div>
          </div>
        )}
      </div>
    </DocumentTitle>
  );
};

export default StreamEditPage;
