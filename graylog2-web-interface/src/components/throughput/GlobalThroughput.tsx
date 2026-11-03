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
import { useTranslation } from 'react-i18next';

import NumberUtils from 'util/NumberUtils';
import { NavItem } from 'components/bootstrap';
import { Icon, Spinner } from 'components/common';
import { itemStateIndicatorContainerSelector } from 'components/common/NavItemStateIndicator';
import { useGlobalThroughput } from 'hooks/useMetrics';

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

    > span:first-of-type {
      grid-area: 1 / 1 / 2 / 2;
      text-align: right;
      padding-left: 3px;
    }

    > .material-symbols-rounded {
      grid-area: 1 / 2 / 2 / 3;
      align-self: center;
      justify-self: center;
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
  const { t } = useTranslation('system');
  const { input, output: outputVal, isLoading } = useGlobalThroughput();
  let output = <Spinner text="" />;

  if (!isLoading) {
    const inputNumeral = NumberUtils.formatNumber(input);
    const outputNumeral = NumberUtils.formatNumber(outputVal);

    output = (
      <ContentWrap aria-label={t('global_throughput.aria_label', { input: inputNumeral, output: outputNumeral })}>
        <ThroughputData $dataIn>
          <span>{inputNumeral}</span>
          <Icon name="arrow_downward" size="xs" title={t('global_throughput.in_title')} />
        </ThroughputData>
        <ThroughputData>
          <span>{outputNumeral}</span>
          <Icon name="arrow_upward" size="xs" title={t('global_throughput.out_title')} />
        </ThroughputData>
      </ContentWrap>
    );
  }

  return <ThroughputNavItem {...props}>{output}</ThroughputNavItem>;
};

export default GlobalThroughput;
