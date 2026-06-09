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

import { Badge } from 'components/bootstrap';
import { Icon } from 'components/common';
import { formatProcessingTime, processingTimeSeverity } from 'components/streams/StreamsOverview/formatProcessingTime';

type Props = {
  ms: number;
  title: string;
};

const Inner = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-weight: normal;
  `,
);

const StrongInner = styled(Inner)`
  font-weight: bold;
`;

const ProcessingTimeBadge = ({ ms, title }: Props) => {
  const severity = processingTimeSeverity(ms);
  const formatted = formatProcessingTime(ms);

  if (severity === 'danger') {
    return (
      <Badge bsStyle="danger" title={title}>
        <StrongInner>
          <Icon name="error" size="sm" />
          {formatted}
        </StrongInner>
      </Badge>
    );
  }

  if (severity === 'warning') {
    return (
      <Badge bsStyle="warning" title={title}>
        <StrongInner>
          <Icon name="warning" size="sm" />
          {formatted}
        </StrongInner>
      </Badge>
    );
  }

  return <Badge title={title}>{formatted}</Badge>;
};

export default ProcessingTimeBadge;
