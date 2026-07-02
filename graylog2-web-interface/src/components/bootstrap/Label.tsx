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

import Badge from './Badge';

const Label = styled(Badge)(
  ({ theme }) => css`
    border-radius: 3px;
    font-weight: normal;
    padding-left: ${theme.spacings.xs};
    padding-right: ${theme.spacings.xs};
    text-align: center;

    .mantine-Badge-label {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: ${theme.spacings.xxs};
    }

    /* When a label's content is wrapped in a <span> (e.g. to carry a title tooltip), that span
       becomes a flex item of the label, so it needs to be its own single-line ellipsis context —
       otherwise long text overflows instead of truncating when the label is width-constrained. */
    .mantine-Badge-label > span {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  `,
);

export default Label;
