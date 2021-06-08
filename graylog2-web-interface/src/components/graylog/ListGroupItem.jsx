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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';

const RefContainer = styled.div(({ theme }) => `
  border: 1px solid ${theme.colors.variant.lighter.default};
  margin-bottom: -1px;

  &:first-child {
    border-top-left-radius: 4px;
    border-top-right-radius: 4px;
  }

  &:last-child {
    margin-bottom: 0;
    border-bottom-right-radius: 4px;
    border-bottom-left-radius: 4px;
  }
`);

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
  border: 0;

  padding: 10px 10px 5px 10px;

  .list-group-item-heading {
    font-size: ${theme.fonts.size.h5};
  }

  .list-group-item-text {
    margin-bottom: 5px;
  }

  a&,
  button& {
    color: ${theme.colors.global.textDefault};

    .list-group-item-heading {
      color: ${theme.colors.variant.darkest.default};
    }

    &:hover:not(.disabled),
    &:focus:not(.disabled) {
      background-color: ${theme.colors.variant.lightest.default};

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
    }

    .list-group-item-text {
      color: ${theme.colors.variant.light.primary};
    }
  }

  ${variantStyles}
`);

const ListGroupItem = forwardRef(({ containerProps, ...rest }, ref) => (
  <RefContainer ref={ref} {...containerProps}>
    <StyledListGroupItem {...rest} />
  </RefContainer>
));

ListGroupItem.propTypes = {
  containerProps: PropTypes.object,
};

ListGroupItem.defaultProps = {
  containerProps: {},
};

export default ListGroupItem;
