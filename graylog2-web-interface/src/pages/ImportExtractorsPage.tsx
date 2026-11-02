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
import { useEffect, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ImportExtractors from 'components/extractors/ImportExtractors';
import type { ParamsContext } from 'routing/withParams';
import withParams from 'routing/withParams';
import { fetchInput } from 'hooks/useInputs';
import type { Input } from 'components/messageloaders/Types';
import useProductName from 'brand-customization/useProductName';
import MarketplaceLink from 'components/support/MarketplaceLink';

type Props = ParamsContext;

const ImportExtractorsPage = ({ params }: Props) => {
  const { t } = useTranslation('extractors');
  const productName = useProductName();
  const [input, setInput] = useState<Input>();

  useEffect(() => {
    fetchInput(params.inputId).then((_input) => setInput(_input));
  }, [params.inputId]);

  const _isLoading = !input;

  if (_isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('pages.import_title', { title: input.title })}>
      <div>
        <PageHeader
          title={
            <span>
              <Trans t={t} i18nKey="pages.import_header" values={{ title: input.title }} components={{ em: <em /> }} />
            </span>
          }>
          <span>
            {t('pages.import_description')}{' '}
            <MarketplaceLink prefix={t('pages.import_marketplace_prefix', { productName })} />
          </span>
        </PageHeader>
        <ImportExtractors input={input} />
      </div>
    </DocumentTitle>
  );
};

export default withParams(ImportExtractorsPage);
