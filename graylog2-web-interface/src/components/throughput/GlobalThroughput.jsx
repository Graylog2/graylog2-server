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

import { useStore } from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import NumberUtils from 'util/NumberUtils';
import { NavItem } from 'components/graylog';
import { Spinner } from 'components/common';

const GlobalThroughputStore = StoreProvider.getStore('GlobalThroughput');

const ThroughputNavItem = styled(NavItem)`
  > a {
    padding-top: 10px !important;
    padding-bottom: 10px !important;
    display: flex !important;
    align-items: center;
    height: 50px;

    @media (max-width: 991px) {
      height: auto;
      display: block;
    }
  }
`;

const ContentWrap = styled.strong`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr 1fr;
  grid-column-gap: 0;
  grid-row-gap: 0;
  height: 30px;

  @media (max-width: 991px) {
    height: auto;
    display: block;

    &::before {
      content: attr(aria-label);
      font-weight: normal;
    }

    span,
    &::after {
      display: none;
    }
  }
`;

const ThroughputData = styled.span(({ dataIn, theme }) => css`
  font-size: ${theme.fonts.size.small};
  line-height: 13px;
  grid-area: ${dataIn ? '1 / 1 / 2 / 2' : '2 / 1 / 3 / 2'};
  display: grid;
  grid-template-columns: 1fr 1.75em;
  grid-template-rows: 1fr 1px;
  grid-column-gap: 3px;
  grid-row-gap: 0;
  color: ${theme.colors.global.textDefault};

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
    ${dataIn && `
      content: ' ';
      min-height: 1px;
      background-color: ${theme.colors.variant.light.default};
      display: block;
      grid-area: 2 / 1 / 3 / 3;
    `}
  }
`);

const GlobalThroughput = (props) => {
  const { throughput } = useStore(GlobalThroughputStore);
  let output = <Spinner text="" />;

  if (!throughput.loading) {
    const inputNumeral = NumberUtils.formatNumber(throughput.input);
    const outputNumeral = NumberUtils.formatNumber(throughput.output);

    output = (
      <ContentWrap aria-label={`In ${inputNumeral} / Out ${outputNumeral} msg/s`}>
        <ThroughputData dataIn>
          <span>{inputNumeral}</span> <i>in</i>
        </ThroughputData>
        <ThroughputData>
          <span>{outputNumeral}</span> <i>out</i>
        </ThroughputData>
      </ContentWrap>
    );
  }

  return (
    <ThroughputNavItem {...props}>
      {output}
    </ThroughputNavItem>
  );
};

export default GlobalThroughput;
