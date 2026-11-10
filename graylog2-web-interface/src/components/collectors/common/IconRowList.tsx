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
import styled, { css } from 'styled-components';

/**
 * Row primitives for icon-annotated lists (an icon followed by text per row), shared between the
 * activity feed entries and the drawer's queued-actions list so they render identically.
 */
export const IconRowList = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

export const IconRow = styled.li(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    padding: ${theme.spacings.xs} 0;
  `,
);

/** An {@link IconRow} with a divider between rows. */
export const DividedIconRow = styled(IconRow)(
  ({ theme }) => css`
    border-bottom: 1px solid ${theme.colors.gray[90]};

    &:last-child {
      border-bottom: none;
    }
  `,
);
