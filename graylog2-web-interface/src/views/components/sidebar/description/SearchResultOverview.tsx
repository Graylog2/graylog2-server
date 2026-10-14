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
import isEmpty from 'lodash/isEmpty';
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Timestamp } from 'components/common';
import { formatNumber } from 'util/NumberFormatting';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import type QueryResult from 'views/logic/QueryResult';

const EffectiveTimeRange = styled.div`
  margin-bottom: 10px;
`;

const EffectiveTimeRangeTable = styled.table`
  margin-bottom: 5px;

  td:first-child {
    padding-right: 10px;
  }
`;

type Props = {
  results: QueryResult;
};

const SearchResultOverview = ({ results }: Props) => {
  const { t } = useTranslation('search');
  const { timerange: globalOverrideTimeRange } = useGlobalOverride() ?? {};
  const viewType = useViewType();

  if (isEmpty(results)) {
    return <i>{t('sidebar_description.no_query')}</i>;
  }

  const { timestamp, duration, effectiveTimerange, searchTypes } = results;
  const total = searchTypes && Object.values(searchTypes)?.[0]?.total;
  const isVariesPerWidget = viewType === View.Type.Dashboard && !globalOverrideTimeRange;

  return (
    <>
      <p>
        {t('sidebar_description.query_executed_in')} <br />
        <Trans
          t={t}
          i18nKey="search_result_overview.duration_at"
          values={{ duration: formatNumber(duration, { digits: 0 }) }}
          components={{ timestamp: <Timestamp dateTime={timestamp} /> }}
        />
      </p>
      <EffectiveTimeRange>
        {t('sidebar_description.effective_time_range')}
        <br />
        {isVariesPerWidget ? (
          <i>{t('sidebar_description.varies_per_widget')}</i>
        ) : (
          <EffectiveTimeRangeTable>
            <tbody>
              <tr>
                <td>{t('sidebar_description.time_from')}</td>
                <td aria-label={t('search_result_overview.effective_from')}>
                  <Timestamp dateTime={effectiveTimerange.from} format="complete" />
                </td>
              </tr>
              <tr>
                <td>{t('sidebar_description.time_to')}</td>
                <td aria-label={t('search_result_overview.effective_to')}>
                  <Timestamp dateTime={effectiveTimerange.to} format="complete" />
                </td>
              </tr>
            </tbody>
          </EffectiveTimeRangeTable>
        )}
      </EffectiveTimeRange>
      <p>
        {t('sidebar_description.total_results')}
        <br />
        {isVariesPerWidget ? <i>{t('sidebar_description.varies_per_widget')}</i> : formatNumber(total, { digits: 0 })}
      </p>
    </>
  );
};

export default SearchResultOverview;
