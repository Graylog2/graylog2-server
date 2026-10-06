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

// The clickable line of a <details>: a twistie (as on the table rows: right = closed, down = open) in link colour,
// underlined on hover, so it reads as something to open.
const DisclosureToggle = styled.summary(
  ({ theme }) => css`
    display: block;
    list-style: none;
    cursor: pointer;
    color: ${theme.colors.global.link};

    &::-webkit-details-marker {
      display: none;
    }

    &::before {
      content: '▸';
      display: inline-block;
      width: 1.2em;
    }

    details[open] > &::before {
      content: '▾';
    }

    &:hover {
      text-decoration: underline;
    }
  `,
);

export default DisclosureToggle;
