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
import Widget from 'views/logic/widgets/Widget';
import timerangeToString from 'views/logic/queries/TimeRangeToString';
import { DEFAULT_TIMERANGE } from 'views/Constants';

type Props = {
  className?: string,
  widget: Widget,
};

const Wrapper = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.tiny};
  color: ${theme.colors.gray[30]};
  width: max-content;
`);

const TimerangeInfo = ({ className, widget }: Props) => {
  const configuredTimerange = timerangeToString(widget.timerange || DEFAULT_TIMERANGE);

  return (
    <Wrapper className={className}>
      <TextOverflowEllipsis>
        {configuredTimerange}
      </TextOverflowEllipsis>
    </Wrapper>
  );
};

TimerangeInfo.defaultProps = {
  className: undefined,
};

export default TimerangeInfo;
