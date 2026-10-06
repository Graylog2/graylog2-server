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
import React from 'react';
import { Tabs as MantineTabs } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledTab = styled(MantineTabs.Tab)`
  height: 40px;
`;

const StyledPanel = styled(MantineTabs.Panel)(
  ({ theme }) => css`
    padding: ${theme.spacings.sm};
  `,
);

type Props = React.PropsWithChildren<{
  defaultValue?: string;
  value?: string | null;
  onChange?: (value: string | null) => void;
  variant?: 'default' | 'outline' | 'pills';
  className?: string;
}>;

const Tabs = ({
  children = undefined,
  defaultValue = undefined,
  value = undefined,
  onChange = undefined,
  variant = 'outline',
  className = undefined,
}: Props) => (
  <MantineTabs defaultValue={defaultValue} value={value} onChange={onChange} variant={variant} className={className}>
    {children}
  </MantineTabs>
);

Tabs.List = MantineTabs.List;
Tabs.Tab = StyledTab;
Tabs.Panel = StyledPanel;

/** @component */
export default Tabs;
