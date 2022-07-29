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

import { TimeUnit } from 'components/common';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import {
  SummaryContainer,
  SummaryRow,
  Title,
  Value,
} from './SummaryComponents.styled';

type Props = {
  cache: LookupTableCache,
};

const CaffeineCacheSummary = ({ cache }: Props) => {
  return (
    <SummaryContainer>
      <SummaryRow>
        <Title>Maximum entries:</Title>
        <Value style={{ borderBottom: '1px solid #eee' }}>{cache.config.max_size}</Value>
      </SummaryRow>
      <SummaryRow>
        <Title>Expire after access:</Title>
        <Value style={{ borderBottom: '1px solid #eee' }}>
          <TimeUnit value={cache.config.expire_after_access}
                    unit={cache.config.expire_after_access_unit} />
        </Value>
      </SummaryRow>
      <SummaryRow>
        <Title>Expire after write:</Title>
        <Value>
          <TimeUnit value={cache.config.expire_after_write}
                    unit={cache.config.expire_after_write_unit} />
        </Value>
      </SummaryRow>
    </SummaryContainer>
  );
};

export default CaffeineCacheSummary;
