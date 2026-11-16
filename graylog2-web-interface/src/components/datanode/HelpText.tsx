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

const HelpText = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

export const HelpTextHeading = styled.h4`
  margin: 0;
  color: inherit;
  font-size: inherit;
`;

export const HelpTextList = styled.ul(
  ({ theme }) => css`
    margin: ${theme.spacings.xs} 0 0;
    padding-left: ${theme.spacings.lg};
    list-style-position: outside;
    list-style-type: disc;

    > li {
      display: list-item;
      margin-bottom: ${theme.spacings.xxs};
    }

    > li:last-child {
      margin-bottom: 0;
    }
  `,
);

export default HelpText;
