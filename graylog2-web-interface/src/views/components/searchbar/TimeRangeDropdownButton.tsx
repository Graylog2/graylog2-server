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
import styled from 'styled-components';

import { DropdownButton } from 'components/graylog';
import { Icon } from 'components/common';

const StyledDropdownButton = styled(DropdownButton)`
  padding: 6px 7px;
  margin-right: 5px;
`;

type Props = {
  onSelect: (newType: string) => void,
  children: React.ReactNode,
  disabled?: boolean,
};

const TimeRangeDropdownButton = ({ onSelect, children, disabled, ...rest }: Props) => (
  <StyledDropdownButton {...rest}
                        bsStyle="info"
                        disabled={disabled}
                        id="timerange-type"
                        title={<Icon name="clock" />}
                        onSelect={onSelect}>
    {children}
  </StyledDropdownButton>
);

TimeRangeDropdownButton.defaultProps = {
  disabled: false,
};

export default TimeRangeDropdownButton;
