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
import { useTranslation } from 'react-i18next';

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
    <DocumentTitle title={t('streams.outputs_for_stream', { streamTitle: stream.title })}>
      <div>
        <ContentHeadRow className="content">
          <Col md={10}>
            <h1 className="page-title">{t('streams.outputs_for_stream', { streamTitle: stream.title })}</h1>

            <p className="description">
              {t('streams.outputs_desc_part1', { productName })}
              <Link to={Routes.SYSTEM.OUTPUTS}>{t('streams.outputs_desc_link_here')}</Link>.{' '}
              <MarketplaceLink prefix={t('streams.outputs_desc_marketplace_prefix')} />
              <br />
              <i>{t('streams.outputs_desc_removing')}</i>
              {t('streams.outputs_desc_part2')}
              <i>{t('streams.outputs_desc_globally')}</i>
              {t('streams.outputs_desc_part3')}
              <Link to={Routes.SYSTEM.OUTPUTS}>{t('streams.outputs_desc_link_global_list')}</Link>.
            </p>
          </Col>
        </ContentHeadRow>
        <OutputsComponent streamId={stream.id} />
      </div>
    </DocumentTitle>
  );
};

export default StreamOutputsPage;
