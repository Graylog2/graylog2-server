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
 * Label/value row primitives for collector detail views, shared between the instance detail drawer
 * and the onboarding summary so both render identically.
 */
export const DetailRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.xs};

    /* Set here rather than on the label so a row's label and value can never disagree. The label
       used to declare this on its own while values inherited the larger body size, which left
       every row visibly mismatched. */
    font-size: ${theme.fonts.size.small};

    /* Long values (certificate hashes, attribute strings) wrap instead of overflowing a narrow
       container; a flex child defaults to min-width auto and would otherwise refuse to shrink. */
    > :last-child {
      min-width: 0;
      overflow-wrap: anywhere;
    }
  `,
);

/** The leading label of a {@link DetailRow}. Reserves a fixed width so values line up. */
export const DetailLabel = styled.span`
  font-weight: 500;
  min-width: 120px;
`;
