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
// import * as React from 'react';
// import styled, { css } from 'styled-components';
// // eslint-disable-next-line no-restricted-imports
// import { Alert as BootstrapAlert } from 'react-bootstrap';
// import type { ColorVariant } from '@graylog/sawmill';
//
// interface Props {
//   bsStyle: ColorVariant,
//   children: React.ReactNode,
//   onDismiss?: () => void,
// }
//
// const StyledAlert = styled(BootstrapAlert)<{ bsStyle: ColorVariant }>(({ bsStyle = 'info', theme }) => {
//   const borderColor = theme.colors.variant.lighter[bsStyle];
//   const backgroundColor = theme.colors.variant.lightest[bsStyle];
//
//   return css`
//     background-color: ${backgroundColor};
//     border-color: ${borderColor};
//     color: ${theme.utils.contrastingColor(backgroundColor)};
//
//     a:not(.btn) {
//       color: ${theme.utils.contrastingColor(backgroundColor, 'AA')};
//       font-weight: bold;
//       text-decoration: underline;
//
//       &:hover,
//       &:focus,
//       &:active {
//         color: ${theme.utils.contrastingColor(backgroundColor)};
//       }
//
//       &:hover,
//       &:focus {
//         text-decoration: none;
//       }
//     }
//
//     &.alert-dismissible {
//       .close {
//         top: -9px;
//       }
//     }
// `;
// });
//
// const Alert = ({ bsStyle, ...rest }: Props) => <StyledAlert bsStyle={bsStyle} {...rest} />;
//
// Alert.defaultProps = {
//   onDismiss: undefined,
// };
//
// export default Alert;

import * as React from 'react';
import styled, { css, useTheme } from 'styled-components';
import { Alert as MantineAlert } from '@mantine/core';
import type { ColorVariant } from '@graylog/sawmill';

const StyledAlert = styled(MantineAlert)(({ theme }) => css`
  margin: ${theme.mantine.spacing.md} 0;
`);

type Props = {
  children: React.ReactNode,
  bsStyle: ColorVariant,
  withCloseButton?: boolean
  onClose?: () => void,
  icon?: React.ReactNode,
};

const Alert = ({ children, bsStyle, withCloseButton, onClose, icon }: Props) => {
  const theme = useTheme();
  const alertStyles = () => ({
    message: {
      fontSize: theme.mantine.fontSizes.md,
    },
  });

  return (
    <StyledAlert icon={icon}
                 styles={alertStyles}
                 color={bsStyle}
                 withCloseButton={withCloseButton}
                 onClose={onClose}>
      {children}
    </StyledAlert>
  );
};

Alert.defaultProps = {
  onClose: undefined,
  withCloseButton: false,
  icon: undefined,
};

export default Alert;
