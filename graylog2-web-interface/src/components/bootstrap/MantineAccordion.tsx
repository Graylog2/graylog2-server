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
import { Accordion } from '@mantine/core';
import { useTheme } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

const getStyles = (theme: DefaultTheme, options: { noBackground?: boolean }) => ({
  root: {
    width: '100%',
  },
  item: {
    backgroundColor: options.noBackground ? 'transparent' : theme.colors.background.body,
    borderRadius: theme.spacings.xs,
  },
});

export type ItemType = {
  value: string;
  label: React.ReactNode;
  content: React.ReactNode;
};

type Props = {
  accordionItems: Array<ItemType>;
  defaultValue: string;
  chevronPosition?: 'left' | 'right';
  variant?: 'default' | 'contained' | 'separated';
  noBackground?: boolean;
};

const MantineAccordion = ({
  accordionItems,
  defaultValue,
  chevronPosition = 'left',
  variant = 'default',
  noBackground = false,
}: Props) => {
  const theme = useTheme();
  const styles = getStyles(theme, { noBackground });
  const items = accordionItems.map((item) => (
    <Accordion.Item key={item.value} value={item.value}>
      <Accordion.Control>{item.label}</Accordion.Control>
      <Accordion.Panel>{item.content}</Accordion.Panel>
    </Accordion.Item>
  ));

  return (
    <Accordion styles={styles} variant={variant} chevronPosition={chevronPosition} defaultValue={defaultValue}>
      {items}
    </Accordion>
  );
};

export default MantineAccordion;
