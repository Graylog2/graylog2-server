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
import styled, { css } from 'styled-components';
import type { PropsWithChildren } from 'react';
import { useCallback, useMemo, useState } from 'react';
import isEmpty from 'lodash/isEmpty';

import { formatNumber } from 'util/NumberFormatting';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import { Icon, Timestamp } from 'components/common';
import { Table } from 'components/bootstrap';
import useViewsSelector from 'views/stores/useViewsSelector';
import { selectCurrentQueryResults } from 'views/logic/slices/viewSelectors';
import type { SearchTypeResult, MessageResult, GenericResult } from 'views/types';
import type { SearchTypeIds } from 'views/logic/views/types';
import Popover from 'components/common/Popover';
import { useIsInteractiveMode } from 'views/components/contexts/InteractiveContext';

type Props = PropsWithChildren & {
  currentWidgetMapping: SearchTypeIds;
};

const TargetContainer = styled.div`
  cursor: pointer;
  display: flex;
  margin-right: 10px;
`;

const StyledTable = styled(Table)<{ $stickyHeader: boolean }>(
  ({ theme }) => css`
    margin-bottom: 0;
    background-color: transparent;

    > tbody td {
      background-color: ${theme.colors.global.contentBackground};
      color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
    }
  `,
);

type WidgetExecutionData = {
  total: number;
  duration: number;
  timestamp: string;
  effectiveTimerange: AbsoluteTimeRange;
};

const StyledIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    margin-left: 5px;
  `,
);

const HelpPopover = ({ widgetExecutionData }: { widgetExecutionData: WidgetExecutionData }) => {
  const { t } = useTranslation('widgets');

  return (
    <StyledTable condensed>
      <tbody>
        <tr>
          <td>
            <i>{t('execution_info.executed_at')}</i>
          </td>
          <td aria-label={t('execution_info.executed_at_label')}>
            <Timestamp dateTime={widgetExecutionData?.timestamp} />
          </td>
        </tr>
        <tr>
          <td>
            <i>{t('execution_info.executed_in')}</i>{' '}
          </td>
          <td>
            {t('execution_info.duration', { duration: formatNumber(widgetExecutionData?.duration, { digits: 0 }) })}
          </td>
        </tr>
        <tr>
          <td colSpan={2}>
            <i>{t('execution_info.effective_time_range')}</i>
          </td>
        </tr>
        <tr>
          <td>{t('execution_info.from')}</td>
          <td aria-label={t('execution_info.from_label')}>
            <Timestamp dateTime={widgetExecutionData?.effectiveTimerange?.from} format="complete" />
          </td>
        </tr>
        <tr>
          <td>{t('execution_info.to')}</td>
          <td aria-label={t('execution_info.to_label')}>
            <Timestamp dateTime={widgetExecutionData?.effectiveTimerange?.to} format="complete" />
          </td>
        </tr>
        <tr>
          <td>
            <i>{t('execution_info.total_results')}</i>
          </td>
          <td>{formatNumber(widgetExecutionData?.total, { digits: 0 })}</td>
        </tr>
      </tbody>
    </StyledTable>
  );
};

const SearchQueryExecutionInfoHelper = ({ currentWidgetMapping, children }: Props) => {
  const { t } = useTranslation('widgets');
  const [open, setOpen] = useState(false);
  const interactive = useIsInteractiveMode();
  const result = useViewsSelector(selectCurrentQueryResults);
  const currentWidgetSearchType = useMemo<SearchTypeResult>(() => {
    const searchTypeId = currentWidgetMapping?.toJS()?.[0];

    return result?.searchTypes?.[searchTypeId];
  }, [currentWidgetMapping, result?.searchTypes]);

  const widgetExecutionData = useMemo<WidgetExecutionData>(
    () => ({
      effectiveTimerange:
        (currentWidgetSearchType as MessageResult)?.effectiveTimerange ||
        (currentWidgetSearchType as GenericResult)?.effective_timerange,
      total: currentWidgetSearchType?.total,
      duration: result?.duration,
      timestamp: result?.timestamp,
    }),
    [currentWidgetSearchType, result?.duration, result?.timestamp],
  );

  const onToggle = useCallback(() => {
    setOpen((cur) => !cur);
  }, []);

  return interactive ? (
    <Popover position="bottom" opened={open} onChange={setOpen} closeOnClickOutside>
      <Popover.Target>
        <TargetContainer role="presentation" onClick={onToggle}>
          <>
            {children}
            <StyledIcon name="help" />
          </>
        </TargetContainer>
      </Popover.Target>
      <Popover.Dropdown title={t('execution_info.title')}>
        {isEmpty(result) ? (
          <i>{t('execution_info.no_query_executed')}</i>
        ) : (
          <HelpPopover widgetExecutionData={widgetExecutionData} />
        )}
      </Popover.Dropdown>
    </Popover>
  ) : (
    <>{children}</>
  );
};

export default SearchQueryExecutionInfoHelper;
