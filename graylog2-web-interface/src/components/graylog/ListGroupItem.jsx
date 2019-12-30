import React, { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';
import { darken } from 'polished';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const listGroupItemStyles = (hex, variant) => {
  const backgroundColor = util.colorLevel(hex, -9);
  const backgroundColorHover = util.colorLevel(hex, -8);
  const textColor = util.colorLevel(hex, 6);

  return css`
    &.list-group-item-${variant} {
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
          background-color: ${backgroundColorHover};
        }

        &.active,
        &.active:hover,
        &.active:focus {
          color: ${util.readableColor(textColor)};
          background-color: ${textColor};
          border-color: ${textColor};
        }
      }
    }
  `;
};

const StyledListGroupItem = styled(BootstrapListGroupItem)(({ theme }) => css`
  background-color: ${theme.color.primary.due};
  border-color: ${theme.color.secondary.tre};

  .list-group-item-heading {
    font-weight: bold;
  }

  &.disabled,
  &.disabled:hover,
  &.disabled:focus {
    color: ${util.contrastingColor(theme.color.secondary.due, 'AA')};
    background-color: ${theme.color.secondary.due};

    .list-group-item-heading {
      color: inherit;
      font-weight: bold;
    }

    .list-group-item-text {
      color: ${util.contrastingColor(theme.color.secondary.due, 'AA')};
    }
  }

  &.active,
  &.active:hover,
  &.active:focus {
    color: ${util.readableColor(theme.color.tertiary.quattro)};
    border-color: ${theme.color.tertiary.quattro};
    background-color: ${theme.color.tertiary.quattro};

    .list-group-item-heading,
    .list-group-item-heading > small,
    .list-group-item-heading > .small {
      color: inherit;
      font-weight: bold;
    }

    .list-group-item-text {
      color: ${util.contrastingColor(theme.color.tertiary.quattro)};
    }
  }

  a&,
  button& {
    color: ${theme.color.primary.tre};

    .list-group-item-heading {
      color: ${util.readableColor(theme.color.primary.due)};
      font-weight: bold;
    }

    &:hover:not(.disabled),
    &:focus:not(.disabled) {
      background-color: ${theme.color.secondary.due};
      color: ${util.readableColor(theme.color.secondary.due, theme.color.tertiary.quattro)};

      &.active {
        color: ${util.readableColor(theme.color.tertiary.quattro)};
        border-color: ${theme.color.tertiary.quattro};
        background-color: ${theme.color.tertiary.quattro};
      }
    }
  }

  ${bsStyleThemeVariant(listGroupItemStyles)}
`);

const ListGroupItem = forwardRef(({ bsStyle, ...props }, ref) => {
  return <StyledListGroupItem bsStyle={bsStyle} ref={ref} {...props} />;
});

ListGroupItem.propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info']),
};

ListGroupItem.defaultProps = {
  bsStyle: undefined,
};

export default ListGroupItem;
