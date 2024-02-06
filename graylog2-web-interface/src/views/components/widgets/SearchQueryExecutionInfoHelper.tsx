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
import styled from 'styled-components';
import { useCallback, useMemo, useState } from 'react';
import numeral from 'numeral';

import { Icon, Timestamp } from 'components/common';
import { Table, Button } from 'components/bootstrap';
import useAppSelector from 'stores/useAppSelector';
import { selectCurrentQueryResults } from 'views/logic/slices/viewSelectors';
import type { MessageResult, SearchTypeResult, SearchTypeResultTypes } from 'views/types';
import type { SearchTypeIds } from 'views/logic/views/types';
import OverlayDropdown from 'components/common/OverlayDropdown';

type Props = {
  currentWidgetMapping: SearchTypeIds,
};

const PopoverContainer = styled.div`
  max-width: 500px;
  padding: 5px;
`;

const QueryHelpButton = styled(Button)`
  padding: 6px 8px;
`;

type WidgetExecutionData = {
  total: number,
  duration: number,
  timestamp: string,
  effectiveTimerange: SearchTypeResult['effective_timerange'] | MessageResult['effectiveTimerange']
}
const HelpPopover = ({ widgetExecutionData }: { widgetExecutionData: WidgetExecutionData}) => (
  <PopoverContainer>
    <p><strong>Execution Info</strong></p>
    <Table condensed>
      <tbody>
        <tr>
          <td><i>Executed at:</i></td>
          <td aria-label="Executed at"><Timestamp dateTime={widgetExecutionData?.timestamp} /></td>
        </tr>
        <tr>
          <td><i>Executed in:</i> </td>
          <td>{numeral(widgetExecutionData?.duration).format('0,0')}ms</td>
        </tr>
        <tr>
          <td colSpan={2}><i>Effective time range:</i></td>
        </tr>
        <tr>
          <td>From</td>
          <td aria-label="Effective time range from"><Timestamp dateTime={widgetExecutionData?.effectiveTimerange?.from} format="complete" /></td>
        </tr>
        <tr>
          <td>To</td>
          <td aria-label="Effective time range to"><Timestamp dateTime={widgetExecutionData?.effectiveTimerange?.to} format="complete" /></td>
        </tr>
        <tr>
          <td><i>Total results:</i></td>
          <td>{numeral(widgetExecutionData?.total).format('0,0')}</td>
        </tr>
      </tbody>
    </Table>
  </PopoverContainer>
);

const SearchQueryExecutionInfoHelper = ({ currentWidgetMapping }: Props) => {
  const [open, setOpen] = useState(false);
  const result = useAppSelector(selectCurrentQueryResults);
  const currentWidgetSearchType = useMemo<SearchTypeResultTypes[keyof SearchTypeResultTypes]>(() => {
    const searchTypeId = currentWidgetMapping?.toJS()?.[0];

    return result?.searchTypes?.[searchTypeId];
  }, [currentWidgetMapping, result?.searchTypes]);

  const widgetExecutionData = useMemo<WidgetExecutionData>(() => ({
    effectiveTimerange: (currentWidgetSearchType as MessageResult)?.effectiveTimerange || (currentWidgetSearchType as SearchTypeResult)?.effective_timerange,
    total: currentWidgetSearchType?.total,
    duration: result?.duration,
    timestamp: result?.timestamp,

  }), [currentWidgetSearchType, result?.duration, result?.timestamp]);

  const onMenuToggle = useCallback(() => {
    setOpen((cur) => !cur);
  }, []);

  return (
    <OverlayDropdown show={open}
                     toggleChild={<QueryHelpButton bsStyle="link"><Icon name="question-circle" /></QueryHelpButton>}
                     placement="right"
                     onToggle={onMenuToggle}
                     menuContainer={null}>
      <HelpPopover widgetExecutionData={widgetExecutionData} />
    </OverlayDropdown>
  );
};

export default SearchQueryExecutionInfoHelper;
