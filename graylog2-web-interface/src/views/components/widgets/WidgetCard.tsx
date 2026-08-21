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

const CardContainer = styled.div<{ $clickable: boolean }>`
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: ${({ theme }) => theme.colors.global.contentBackground};
  border-radius: ${({ theme }) => theme.spacings.xs};
  /* Matches the real search widget's container padding (WidgetFrame). */
  padding: 7px 9px 6px;

  ${({ $clickable, theme }) =>
    $clickable &&
    css`
      cursor: pointer;

      &:hover {
        outline: 1px solid ${theme.colors.link.hover};
      }
    `}
`;

const HeaderRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacings.xs};
  margin-bottom: 5px;
  min-height: 25px;
`;

/* Matches the real search widget's title styling (WidgetHeader's Title). */
const Headline = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  `,
);

/* Matches the bottom-right time range text real search widgets show (TimerangeInfo). */
const TimeRangeInfo = styled.small`
  color: ${({ theme }) => theme.colors.text.secondary};
  white-space: nowrap;
  align-self: flex-end;
`;

type Props = React.PropsWithChildren<{
  headline: string;
  timeRangeInfo?: React.ReactNode;
  actions?: React.ReactNode;
  onClick?: () => void;
  className?: string;
}>;

const WidgetCard = ({
  headline,
  timeRangeInfo = undefined,
  actions = undefined,
  onClick = undefined,
  className = undefined,
  children = undefined,
}: Props) => (
  <CardContainer $clickable={!!onClick} onClick={onClick} className={className}>
    <HeaderRow>
      <Headline title={headline}>{headline}</Headline>
      {actions}
    </HeaderRow>
    {children}
    {timeRangeInfo && <TimeRangeInfo>{timeRangeInfo}</TimeRangeInfo>}
  </CardContainer>
);

export default WidgetCard;
