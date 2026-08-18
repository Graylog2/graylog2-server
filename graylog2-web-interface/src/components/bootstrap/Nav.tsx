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
import styled from 'styled-components';

const Nav = styled.ul`
  display: flex;
  align-items: center;

  /* Bootstrap's base stylesheet gives every list a bottom margin, which its own nav rules used to
     cancel. Without this a navigation bar pushes whatever follows it down by 10px. */
  margin: 0;
  padding-left: 0;
`;

/** @component */
export default Nav;
