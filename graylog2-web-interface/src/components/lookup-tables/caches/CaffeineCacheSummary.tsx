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
import { useTranslation } from 'react-i18next';

import { TimeUnit } from 'components/common';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import { SummaryContainer, SummaryRow, Title, Value } from './SummaryComponents.styled';

type Props = {
  cache: LookupTableCache;
};

const CaffeineCacheSummary = ({ cache }: Props) => {
  const { t } = useTranslation('lookup_tables');

  return (
    <SummaryContainer>
      <SummaryRow>
        <Title>{t('max_entries')}</Title>
        <Value style={{ borderBottom: '1px solid #eee' }}>{cache.config.max_size}</Value>
      </SummaryRow>
      <SummaryRow>
        <Title>{t('expire_after_access')}</Title>
        <Value style={{ borderBottom: '1px solid #eee' }}>
          <TimeUnit value={cache.config.expire_after_access} unit={cache.config.expire_after_access_unit} />
        </Value>
      </SummaryRow>
      <SummaryRow>
        <Title>{t('expire_after_write')}</Title>
        <Value>
          <TimeUnit value={cache.config.expire_after_write} unit={cache.config.expire_after_write_unit} />
        </Value>
      </SummaryRow>
      <SummaryRow>
        <Title>{t('ttl_empty_results')}</Title>
        <Value>
          <TimeUnit value={cache.config.ttl_empty || 0} unit={cache.config.ttl_empty_unit || 'SECONDS'} />
        </Value>
      </SummaryRow>
    </SummaryContainer>
  );
};

export default CaffeineCacheSummary;
