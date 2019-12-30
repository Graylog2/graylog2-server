import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';
import { darken } from 'polished';

import { useTheme } from 'theme/GraylogThemeContext';
import { colorLevel } from 'theme/util';
import contrastingColor from 'util/contrastingColor';
import bsStyleThemeVariant from './variants/bsStyle';

const listGroupItemStyles = (hex) => {
  const backgroundColor = colorLevel(hex, -9);
  const textColor = colorLevel(hex, 6);

  return css`
    color: ${textColor};
    background-color: ${backgroundColor};

    &.list-group-item-action {
      &:hover,
      &:focus {
        color: ${textColor};
        background-color: ${darken(0.05, backgroundColor)};
      }

      &.active {
        color: ${contrastingColor(textColor)};
        background-color: ${textColor};
        border-color: ${textColor};
      }
    }

    &.list-group-item {
      color: ${textColor};
      background-color: ${backgroundColor};

      &.active {
        color: ${contrastingColor(hex)};
        background-color: ${hex};
        border-color: ${hex};
      }
    }
  `;
};

const ListGroupItem = forwardRef(({ bsStyle, ...props }, ref) => {
  const { colors } = useTheme();
  const StyledListGroupItem = useMemo(
    () => {
      return styled(BootstrapListGroupItem)`
        &.list-group-item-action {
          color: ${colors.primary.tre};

          &:hover,
          &:focus {
            color: ${colors.primary.tre};
            background-color: ${colors.secondary.due};
          }

          &:active {
            color: ${contrastingColor(colors.secondary.tre)};
            background-color: ${colors.secondary.tre};
          }
        }

        &.list-group-item {
          background-color: ${colors.primary.due};
          border-color: ${colors.secondary.tre};

          &.disabled,
          &:disabled {
            color: ${colors.primary.tre};
            background-color: ${colors.primary.due};
          }
        }

        ${bsStyleThemeVariant(listGroupItemStyles)}
      `;
    },
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
