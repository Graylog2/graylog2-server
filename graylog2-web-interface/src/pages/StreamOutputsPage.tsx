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
import { useParams } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';

import { Link, ContentHeadRow, DocumentTitle, Spinner } from 'components/common';
import { Col } from 'components/bootstrap';
import OutputsComponent from 'components/outputs/OutputsComponent';
import Routes from 'routing/Routes';
import useStream from 'components/streams/hooks/useStream';
import useProductName from 'brand-customization/useProductName';
import MarketplaceLink from 'components/support/MarketplaceLink';

const StreamOutputsPage = () => {
  const { t } = useTranslation('streams');
  const productName = useProductName();
  const { streamId } = useParams();
  const { data: stream } = useStream(streamId);

  if (!stream) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('stream_outputs.title', { streamTitle: stream.title })}>
      <div>
        <ContentHeadRow className="content">
          <Col md={10}>
            <h1 className="page-title">{t('stream_outputs.title', { streamTitle: stream.title })}</h1>

            <p className="description">
              <Trans
                t={t}
                i18nKey="stream_outputs.description"
                values={{ productName }}
                components={{ outputsLink: <Link to={Routes.SYSTEM.OUTPUTS} /> }}
              />{' '}
              <MarketplaceLink prefix={t('stream_outputs.marketplace_prefix')} />
              <br />
              <Trans
                t={t}
                i18nKey="stream_outputs.removing_description"
                components={{ emphasis: <i />, outputsLink: <Link to={Routes.SYSTEM.OUTPUTS} /> }}
              />
            </p>
          </Col>
        </ContentHeadRow>
        <OutputsComponent streamId={stream.id} />
      </div>
    </DocumentTitle>
  );
};

export default StreamOutputsPage;
