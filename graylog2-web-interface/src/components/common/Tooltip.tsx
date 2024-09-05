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
import { Tooltip as MantineTooltip } from '@mantine/core';
import { useTheme } from 'styled-components';

type Props = React.ComponentProps<typeof MantineTooltip>;

const Tooltip = (props: Props) => {
  const theme = useTheme();
  const styles = () => ({
    tooltip: {
      backgroundColor: theme.colors.global.contentBackground,
      color: theme.colors.global.textDefault,
      fontWeight: 400,
      fontSize: theme.fonts.size.root,
    },
  });

  return <MantineTooltip zIndex="var(--mantine-z-index-max)" styles={styles} {...props} />;
};

export default Tooltip;
