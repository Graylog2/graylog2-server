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
import numeral from 'numeral';
import isEmpty from 'lodash/isEmpty';
import styled from 'styled-components';

import { Timestamp } from 'components/common';
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
  results: QueryResult,
};

const SearchResultOverview = ({ results }: Props) => {
  const { timerange: globalOverrideTimeRange } = useGlobalOverride() ?? {};
  const viewType = useViewType();

  if (isEmpty(results)) {
    return <i>No query executed yet.</i>;
  }

  const { timestamp, duration, effectiveTimerange, searchTypes } = results;
  const total = searchTypes && Object.values(searchTypes)?.[0]?.total;
  const isVariesPerWidget = (viewType === View.Type.Dashboard && !globalOverrideTimeRange);

  return (
    <>
      <p>
        Query executed in <br />
        {numeral(duration).format('0,0')}ms at <Timestamp dateTime={timestamp} />
      </p>
      <EffectiveTimeRange>
        Effective time range<br />
        {isVariesPerWidget ? <i>Varies per widget</i>
          : (
            <EffectiveTimeRangeTable>
              <tbody>
                <tr>
                  <td>From</td>
                  <td aria-label="Effective time range from"><Timestamp dateTime={effectiveTimerange.from} format="complete" /></td>
                </tr>
                <tr>
                  <td>To</td>
                  <td aria-label="Effective time range to"><Timestamp dateTime={effectiveTimerange.to} format="complete" /></td>
                </tr>
              </tbody>
            </EffectiveTimeRangeTable>
          )}
      </EffectiveTimeRange>
      <p>
        Total results<br />
        {isVariesPerWidget ? <i>Varies per widget</i> : numeral(total).format('0,0')}
      </p>
    </>
  );
};

export default SearchResultOverview;
