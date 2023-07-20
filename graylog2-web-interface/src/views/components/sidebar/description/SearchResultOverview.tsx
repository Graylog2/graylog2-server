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
import PropTypes from 'prop-types';
import numeral from 'numeral';
import isEmpty from 'lodash/isEmpty';
import styled from 'styled-components';

import { Timestamp } from 'components/common';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';

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
  results: {
    timestamp?: string,
    duration?: number,
    effectiveTimerange: AbsoluteTimeRange
  },
};

const SearchResultOverview = ({ results }: Props) => {
  const { timerange: globalOverrideTimeRange } = useGlobalOverride() ?? {};
  const viewType = useViewType();

  if (isEmpty(results)) {
    return <i>No query executed yet.</i>;
  }

  const { timestamp, duration, effectiveTimerange } = results;

  return (
    <>
      <p>
        Query executed in <br />
        {numeral(duration).format('0,0')}ms at <Timestamp dateTime={timestamp} />
      </p>
      <EffectiveTimeRange>
        Effective time range<br />
        {(viewType === View.Type.Dashboard && !globalOverrideTimeRange) ? <i>Varies per widget</i>
          : (
            <EffectiveTimeRangeTable>
              <tbody>
                <tr>
                  <td>From</td>
                  <td><Timestamp dateTime={effectiveTimerange.from} format="complete" /></td>
                </tr>
                <tr>
                  <td>To</td>
                  <td><Timestamp dateTime={effectiveTimerange.to} format="complete" /></td>
                </tr>
              </tbody>
            </EffectiveTimeRangeTable>
          )}
      </EffectiveTimeRange>
    </>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
