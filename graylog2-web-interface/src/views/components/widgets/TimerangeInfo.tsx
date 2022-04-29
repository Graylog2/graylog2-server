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

import { TextOverflowEllipsis } from 'components/common';
import type Widget from 'views/logic/widgets/Widget';
import timerangeToString from 'views/logic/queries/TimeRangeToString';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { useStore } from 'stores/connect';
import { SearchStore } from 'views/stores/SearchStore';
import { GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import useUserDateTime from 'hooks/useUserDateTime';
import type { DateTime } from 'util/DateTime';

type Props = {
  className?: string,
  widget: Widget,
  activeQuery?: string,
  widgetId?: string,
};

const Wrapper = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.tiny};
  color: ${theme.colors.gray[30]};
  width: max-content;
`);

const getEffectiveWidgetTimerange = (result, activeQuery, searchTypeId) => result?.results[activeQuery]
  .searchTypes[searchTypeId]?.effective_timerange;

const TimerangeInfo = ({ className, widget, activeQuery, widgetId }: Props) => {
  const { formatTime } = useUserDateTime();
  const { result, widgetMapping } = useStore(SearchStore);
  const globalOverride = useStore(GlobalOverrideStore);

  const toLocalTimeWithMS = (dateTime: DateTime) => formatTime(dateTime, 'complete');
  const toInternalTime = (dateTime: DateTime) => formatTime(dateTime, 'internal');
  const globalTimerangeString = globalOverride?.timerange
    ? `Global Override: ${timerangeToString(globalOverride.timerange, toLocalTimeWithMS)}` : undefined;

  const configuredTimerange = timerangeToString(widget.timerange || DEFAULT_TIMERANGE, toLocalTimeWithMS);

  const searchTypeId = widgetId ? widgetMapping?.get(widgetId)?.first() : undefined;

  const effectiveTimerange = (activeQuery && searchTypeId) ? getEffectiveWidgetTimerange(result, activeQuery, searchTypeId) : undefined;
  const effectiveTimerangeString = effectiveTimerange ? timerangeToString(effectiveTimerange, toInternalTime) : 'Effective widget time range is currently not available.';

  return (
    <Wrapper className={className}>
      <TextOverflowEllipsis titleOverride={effectiveTimerangeString}>
        {globalTimerangeString || configuredTimerange}
      </TextOverflowEllipsis>
    </Wrapper>
  );
};

TimerangeInfo.defaultProps = {
  className: undefined,
  activeQuery: undefined,
  widgetId: undefined,
};

export default TimerangeInfo;
