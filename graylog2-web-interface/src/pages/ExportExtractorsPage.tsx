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
import { useQuery } from '@tanstack/react-query';
import { Trans, useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ExportExtractors from 'components/extractors/ExportExtractors';
import { fetchInput } from 'hooks/useInputs';
import useParams from 'routing/useParams';
import MarketplaceLink from 'components/support/MarketplaceLink';

const ExportExtractorsPage = () => {
  const { t } = useTranslation('extractors');
  const { inputId } = useParams();
  const { data: input } = useQuery({
    queryKey: ['inputs', inputId],
    queryFn: () => fetchInput(inputId),
  });

  if (!input) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('pages.export_title', { title: input.title })}>
      <div>
        <PageHeader
          title={
            <span>
              <Trans
                t={t}
                i18nKey="pages.export_header"
                values={{ title: input.title }}
                components={{ em: <em /> }}
              />
            </span>
          }>
          <span>
            <MarketplaceLink prefix={t('pages.export_marketplace_prefix')} />
          </span>
        </PageHeader>
        <ExportExtractors id={input.id} />
      </div>
    </DocumentTitle>
  );
};

export default ExportExtractorsPage;
