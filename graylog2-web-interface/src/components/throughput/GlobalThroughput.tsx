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
import styled, { css } from 'styled-components';

import NumberUtils from 'util/NumberUtils';
import { NavItem } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { itemStateIndicatorContainerSelector } from 'components/common/NavItemStateIndicator';
import { useGlobalThroughput } from 'hooks/useMetrics';
import { NAVBAR_GAP } from 'theme/constants';

// The state indicator wraps the counter in an inline element, whose own box is only as tall as a line
// of text, so the indicator would be drawn through the middle of the counter. Letting it wrap the
// counter as a block puts it along the bottom, where it belongs.
const ThroughputNavItem = styled(NavItem)`
  ${itemStateIndicatorContainerSelector} {
    display: inline-block;
  }
`;

const ContentWrap = styled.strong`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 0;
  height: 30px;
  padding-left: ${NAVBAR_GAP}px;
  padding-right: ${NAVBAR_GAP}px;
`;

const ThroughputData = styled.span<{ $dataIn?: boolean }>(
  ({ $dataIn, theme }) => css`
    font-size: ${theme.fonts.size.small};
    line-height: 1;
    grid-area: ${$dataIn ? '1 / 1 / 2 / 2' : '2 / 1 / 3 / 2'};
    display: grid;
    grid-template-columns: 1fr 1.75em;
    grid-template-rows: 1fr 1px;
    gap: 0 3px;
    color: ${theme.colors.text.primary};

    > span {
      grid-area: 1 / 1 / 2 / 2;
      text-align: right;
      padding-left: 3px;
    }

    > i {
      font-weight: normal;
      grid-area: 1 / 2 / 2 / 3;
    }

    &::after {
      ${$dataIn &&
      css`
        content: ' ';
        min-height: 1px;
        background-color: ${theme.colors.variant.light.default};
        display: block;
        grid-area: 2 / 1 / 3 / 3;
      `}
    }
  `,
);

const GlobalThroughput = (props) => {
  const { input, output: outputVal, isLoading } = useGlobalThroughput();
  let output = <Spinner text="" />;

  if (!isLoading) {
    const inputNumeral = NumberUtils.formatNumber(input);
    const outputNumeral = NumberUtils.formatNumber(outputVal);

    output = (
      <ContentWrap aria-label={`Throughput: In ${inputNumeral} / Out ${outputNumeral} msg/s`}>
        <ThroughputData $dataIn>
          <span>{inputNumeral}</span> <i>in</i>
        </ThroughputData>
        <ThroughputData>
          <span>{outputNumeral}</span> <i>out</i>
        </ThroughputData>
      </ContentWrap>
    );
  }

  return <ThroughputNavItem {...props}>{output}</ThroughputNavItem>;
};

export default GlobalThroughput;
