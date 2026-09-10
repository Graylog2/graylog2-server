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

import { NAV_ITEM_HEIGHT, NAVBAR_GAP } from 'theme/constants';

const Navbar = styled.header(
  ({ theme }) => css`
    position: relative;
    height: ${NAV_ITEM_HEIGHT};
    min-height: auto;
    display: flex;
    align-items: center;
    gap: ${NAVBAR_GAP}px;

    /* The navigation menu collapses into a burger menu instead of wrapping onto a second line,
       which the fixed height could not accommodate anyway. */
    flex-wrap: nowrap;
    padding: 0 15px;
    background-color: ${theme.colors.global.navigationBackground};
    border: 0;
    box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
    margin-bottom: 0;
    font-family: ${theme.fonts.family.navigation};
    font-size: ${theme.fonts.size.navigation};
  `,
);

/** @component */
export default Navbar;
