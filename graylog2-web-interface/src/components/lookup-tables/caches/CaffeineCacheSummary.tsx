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
