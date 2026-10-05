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
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';

// Direct port of Bootstrap 3's `.row`, which is the variant in use per
// `public/stylesheets/bootstrap-config.json`. The row extends 15px outside its
// container via horizontal negative margins so a `<Col>`'s 15px horizontal
// padding aligns the column content with the container's edges, and contains
// its floated column children with a clearfix.
export const RowContentStyles = css(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
    margin-bottom: ${theme.spacings.xs};
    border-radius: 6px;
    box-shadow: rgb(0 0 0 / 4%) 0 3px 5px;
  `,
);

type Props = React.HTMLAttributes<HTMLDivElement> & {
  as?: React.ElementType;
  'data-testid'?: string;
  className?: string;
};

const StyledRow = styled.div`
  margin-left: -15px;
  margin-right: -15px;

  &::before,
  &::after {
    display: table;
    content: ' ';
  }

  &::after {
    clear: both;
  }

  &.content {
    ${RowContentStyles}
  }
`;

const Row = (
  { children = undefined, as = undefined, className = undefined, ...rest }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => (
  <StyledRow ref={ref} as={as} className={`${className ?? ''} row`} {...rest}>
    {children}
  </StyledRow>
);

/** @component */
export default forwardRef(Row);
