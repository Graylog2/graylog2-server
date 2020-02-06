import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';
import { darken } from 'polished';

import { useTheme } from 'theme/GraylogThemeContext';
import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const listGroupItemStyles = (hex, variant) => {
  const backgroundColor = util.colorLevel(hex, -9);
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
          background-color: ${darken(0.2, backgroundColor)};
        }

        &.active,
        &.active:hover,
        &.active:focus {
          color: #fff;
          background-color: ${textColor};
          border-color: ${textColor};
        }
      }
    }
  `;
};

const ListGroupItem = forwardRef(({ bsStyle, ...props }, ref) => {
  const { colors } = useTheme();
  const StyledListGroupItem = useMemo(
    () => styled(BootstrapListGroupItem)`
      background-color: ${colors.primary.due};
      border-color: ${colors.secondary.tre};

      .list-group-item-heading {
        font-weight: bold;
      }

      &.disabled,
      &.disabled:hover,
      &.disabled:focus {
        color: ${util.contrastingColor(colors.secondary.due, 'AA')};
        background-color: ${colors.secondary.due};

        .list-group-item-heading {
          color: inherit;
          font-weight: bold;
        }

        .list-group-item-text {
          color: ${util.contrastingColor(colors.secondary.due, 'AA')};
        }
      }

      &.active,
      &.active:hover,
      &.active:focus {
        color: ${util.readableColor(colors.tertiary.quattro)};
        border-color: ${colors.tertiary.quattro};
        background-color: ${colors.tertiary.quattro};

        .list-group-item-heading,
        .list-group-item-heading > small,
        .list-group-item-heading > .small {
          color: inherit;
          font-weight: bold;
        }

        .list-group-item-text {
          color: ${util.contrastingColor(colors.tertiary.quattro)};
        }
      }

      a&,
      button& {
        color: ${colors.primary.tre};

        .list-group-item-heading {
          color: ${util.readableColor(colors.primary.due)};
          font-weight: bold;
        }

        &:hover:not(.disabled),
        &:focus:not(.disabled) {
          background-color: ${colors.secondary.due};
          color: ${util.readableColor(colors.secondary.due, colors.tertiary.quattro)};

          &.active {
            color: ${util.readableColor(colors.tertiary.quattro)};
            border-color: ${colors.tertiary.quattro};
            background-color: ${colors.tertiary.quattro};
          }
        }
      }

      ${bsStyleThemeVariant(listGroupItemStyles)}
    `,
    [bsStyle, colors],
  );

  return (
    <StyledListGroupItem bsStyle={bsStyle} ref={ref} {...props} />
  );
});

ListGroupItem.propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info']),
};

ListGroupItem.defaultProps = {
  bsStyle: undefined,
};

export default ListGroupItem;
