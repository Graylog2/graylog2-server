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
import styled, { StyledComponent } from 'styled-components';

import { ThemeInterface } from 'theme';

type Props = {
  children: JSX.Element,
  className?: string,
};

const Conainter: StyledComponent<undefined, ThemeInterface, HTMLDivElement> = styled.div`
  display: flex;

  > div:first-child > div {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
  }

  > div:last-child > div {
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
  }

  > div:not(:first-child) > div {
    border-left: 0;
  }

  > div:not(:first-child):not(:last-child) > div {
    border-radius: 0;
  }
`;

const SelectGroup = ({ children, className }: Props) => <Conainter className={className}>{children}</Conainter>;

SelectGroup.defaultProps = {
  className: undefined,
};

export default SelectGroup;
