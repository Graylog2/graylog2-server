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

import { Nav } from 'components/bootstrap';

/**
 * The list of links to the sections of a configuration page, laid out vertically. react-bootstrap
 * used to do this through its `stacked` prop.
 */
export const SectionNav = styled(Nav)`
  flex-direction: column;
  align-items: stretch;
`;

/**
 * One entry of a `SectionNav`, styled the way react-bootstrap's `nav-pills` used to style it. The
 * page rendering the item decides when it is active, because a section stays active for every path
 * below it, which is more than `LinkContainer` treats as a match.
 */
export const SectionNavItem = styled.li(
  ({ theme }) => css`
    & + & {
      margin-top: 2px;
    }

    > a {
      display: block;
      padding: 10px 15px;
      border-radius: 4px;
      color: ${theme.colors.global.link};

      &:hover,
      &:focus {
        background-color: ${theme.colors.variant.lightest.default};
        text-decoration: none;
      }
    }

    &.active > a {
      &,
      &:hover,
      &:focus {
        color: ${theme.utils.contrastingColor(theme.colors.global.link)};
        background-color: ${theme.colors.global.link};
      }
    }
  `,
);
