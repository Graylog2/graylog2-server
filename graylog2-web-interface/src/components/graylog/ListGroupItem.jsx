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
import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';

const variantStyles = css(({ bsStyle, theme }) => {
  if (!bsStyle) {
    return undefined;
  }

  const backgroundColor = theme.colors.variant.lighter[bsStyle];
  const textColor = theme.utils.readableColor(backgroundColor);

  return css`
    &.list-group-item-${bsStyle} {
      color: ${textColor};
      background-color: ${backgroundColor};

      a&,
      button& {
        color: ${textColor};

        .list-group-item-heading {
          color: inherit;
          font-weight: bold;
        }

        &:hover,
        &:focus {
          color: ${textColor};
          background-color: ${theme.colors.variant.light[bsStyle]};
        }

        &.active,
        &.active:hover,
        &.active:focus {
          color: ${theme.utils.readableColor(theme.colors.variant.light[bsStyle])};
          background-color: ${theme.colors.variant.light[bsStyle]};
          border-color: ${theme.colors.variant.light[bsStyle]};
        }
      }
    }
  `;
});

const StyledListGroupItem = styled(BootstrapListGroupItem)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border-color: ${theme.colors.variant.lighter.default};

  .list-group-item-heading {
    font-weight: bold;
  }

  a&,
  button& {
    color: ${theme.colors.global.link};

    .list-group-item-heading {
      color: ${theme.colors.variant.darkest.default};
      font-weight: bold;
    }

    &:hover:not(.disabled),
    &:focus:not(.disabled) {
      background-color: ${theme.colors.variant.lightest.default};
      color: ${theme.colors.global.linkHover};

      &.active {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.variant.lightest.default};
        border-color: ${theme.colors.variant.lightest.default};
      }

      .list-group-item-heading {
        color: ${theme.utils.readableColor(theme.colors.variant.lightest.default)};
      }
    }
  }

  &.disabled,
  &.disabled:hover,
  &.disabled:focus {
    color: ${theme.colors.variant.default};
    background-color: ${theme.colors.variant.lightest.default};

    .list-group-item-heading {
      color: inherit;
      font-weight: bold;
    }

    .list-group-item-text {
      color: ${theme.colors.variant.default};
    }
  }

  &.active,
  &.active:hover,
  &.active:focus {
    color: ${theme.colors.variant.darker.default};
    background-color: ${theme.colors.variant.lightest.info};
    border-color: ${theme.colors.variant.lightest.info};
    z-index: auto;

    .list-group-item-heading,
    .list-group-item-heading > small,
    .list-group-item-heading > .small {
      color: inherit;
      font-weight: bold;
    }

    .list-group-item-text {
      color: ${theme.colors.variant.light.primary};
    }
  }

  ${variantStyles}
`);

const ListGroupItem = forwardRef((props, ref) => {
  return <StyledListGroupItem {...props} ref={ref} />;
});

export default ListGroupItem;
