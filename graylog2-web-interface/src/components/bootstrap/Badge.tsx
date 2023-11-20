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
// eslint-disable-next-line no-restricted-imports
import { Badge as BootstrapBadge } from 'react-bootstrap';
import type { ColorVariant } from '@graylog/sawmill';

type BadgeProps = {
  bsStyle?: ColorVariant,
};
type MergedProps = React.ComponentProps<typeof BootstrapBadge> & BadgeProps;
const Badge: React.ComponentType<MergedProps> = styled(BootstrapBadge)<BadgeProps>(({ bsStyle, theme }) => {
  if (!bsStyle) {
    return undefined;
  }

  const backgroundColor = theme.colors.variant[bsStyle];
  const textColor = theme.utils.readableColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    color: ${textColor};
`;
});

export default Badge;
export { Badge as StyledBadge };
