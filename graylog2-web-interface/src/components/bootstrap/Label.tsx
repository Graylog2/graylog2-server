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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Label as BootstrapLabel } from 'react-bootstrap';

const getColorStyles = (theme: DefaultTheme, bsStyle: string) => {
  if (!bsStyle) {
    return '';
  }

  const { color, background } = theme.colors.button[bsStyle === 'default' ? 'gray' : bsStyle];

  return css`
    background-color: ${background};
    color: ${color};
    font-weight: normal;
`;
};

type StyledLabelProps = {
  bsStyle?: string,
};
type Props = React.ComponentProps<typeof BootstrapLabel> & StyledLabelProps;
const StyledLabel: React.ComponentType<Props> = styled(BootstrapLabel)<StyledLabelProps>(({ bsStyle, theme }) => css`
  ${getColorStyles(theme, bsStyle)}
  padding: 0.3em 0.6em;
`);

export default StyledLabel;
