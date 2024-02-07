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
import type { ColorVariant } from '@graylog/sawmill';
import { Badge as MantineBadge, useMantineTheme } from '@mantine/core';

type Props = React.PropsWithChildren<{
  bsStyle?: ColorVariant,
  className?: string
  'data-testid'?: string,
  onClick?: () => void
  title?: string,
}>

const Badge = React.forwardRef<HTMLDivElement, Props>(({
  bsStyle,
  className,
  children,
  'data-testid': dataTestid,
  onClick,
  title,
}, ref) => {
  const theme = useMantineTheme();

  const styles = {
    root: {
      color: theme.other.colors.contrast[bsStyle],
    },
    inner: {
      fontSize: theme.fontSizes.sm,
    },
  };

  return (
    <MantineBadge color={bsStyle}
                  className={className}
                  title={title}
                  data-testid={dataTestid}
                  ref={ref}
                  styles={styles}
                  variant="filled"
                  onClick={onClick}>
      {children}
    </MantineBadge>
  );
});

Badge.defaultProps = {
  bsStyle: 'default',
  className: undefined,
  'data-testid': undefined,
  onClick: undefined,
  title: undefined,
};

export default Badge;
