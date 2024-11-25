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
import styled, { css } from 'styled-components';
import type { PropsWithChildren } from 'react';
import { useCallback, useContext, useMemo, useState } from 'react';
import numeral from 'numeral';
import isEmpty from 'lodash/isEmpty';

import { Icon, Timestamp } from 'components/common';
import { Table } from 'components/bootstrap';
import useAppSelector from 'stores/useAppSelector';
import { selectCurrentQueryResults } from 'views/logic/slices/viewSelectors';
import type { MessageResult, SearchTypeResult, SearchTypeResultTypes } from 'views/types';
import type { SearchTypeIds } from 'views/logic/views/types';
import Popover from 'components/common/Popover';
import InteractiveContext from 'views/components/contexts/InteractiveContext';

type Props = PropsWithChildren & {
  currentWidgetMapping: SearchTypeIds,
};

const TargetContainer = styled.div`
  cursor: pointer;
  display: flex;
  margin-right: 10px
`;

const StyledTable = styled(Table)<{ $stickyHeader: boolean }>(({ theme }) => css`
  margin-bottom: 0;
  background-color: transparent;

  > tbody td {
    background-color: ${theme.colors.global.contentBackground};
    color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
  }
`);

type WidgetExecutionData = {
  total: number,
  duration: number,
  timestamp: string,
  effectiveTimerange: SearchTypeResult['effective_timerange'] | MessageResult['effectiveTimerange']
}

const StyledIcon = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.gray[60]}
`);

const HelpPopover = ({ widgetExecutionData }: { widgetExecutionData: WidgetExecutionData}) => (
  <StyledTable condensed>
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
  </StyledTable>
);

const SearchQueryExecutionInfoHelper = ({ currentWidgetMapping, children }: Props) => {
  const [open, setOpen] = useState(false);
  const interactive = useContext(InteractiveContext);
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

  const onClose = useCallback(() => {
    setOpen(false);
  }, []);

  const onToggle = useCallback(() => {
    setOpen((cur) => !cur);
  }, []);

  return interactive ? (
    <Popover position="bottom" opened={open} onClose={onClose} closeOnClickOutside>
      <Popover.Target>
        <TargetContainer role="presentation" onClick={onToggle}>
          <>
            {children}
            <StyledIcon name="help" />
          </>
        </TargetContainer>
      </Popover.Target>
      <Popover.Dropdown title="Execution Info">
        {isEmpty(result) ? <i>No query executed yet.</i> : <HelpPopover widgetExecutionData={widgetExecutionData} />}
      </Popover.Dropdown>
    </Popover>

  ) : <>{children}</>;
};

export default SearchQueryExecutionInfoHelper;
