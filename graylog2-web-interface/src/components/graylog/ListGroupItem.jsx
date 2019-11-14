import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';

import { color, util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const listGroupItemStyles = (hex, variant) => {
  const backgroundColor = util.colorLevel(color.variant[variant], -9);
  const textColor = util.readableColor(backgroundColor);

  return css`
    &.list-group-item-${variant} {
      color: ${textColor};
      background-color: ${backgroundColor};

      a&,
      button& {
        color: ${textColor};

        .list-group-item-heading {
          color: inherit;
        }

        &:hover,
        &:focus {
          color: ${textColor};
          background-color: ${color.variant.light[variant]};
        }

        &.active,
        &.active:hover,
        &.active:focus {
          color: ${util.readableColor(color.variant.light[variant])};
          background-color: ${color.variant.light[variant]};
          border-color: ${color.variant.light[variant]};
        }
      }
    }
  `;
};

const StyledListGroupItem = React.memo(styled(BootstrapListGroupItem)`
  ${bsStyleThemeVariant(listGroupItemStyles, {}, ['success', 'info', 'warning', 'danger'])};
  background-color: ${color.gray[90]};
  border-color: ${color.gray[80]};

  &.disabled,
  &.disabled:hover,
  &.disabled:focus {
    color: ${color.gray[60]};
    background-color: ${color.gray[90]};

    .list-group-item-text {
      color: ${color.gray[60]};
    }
  }

  &.active,
  &.active:hover,
  &.active:focus {
    color: ${color.gray[100]};
    background-color: ${color.variant.primary};
    border-color: ${color.variant.light.primary};

    .list-group-item-text {
      color: ${color.variant.light.primary};
    }
  }

  a&,
  button& {
    color: ${color.global.link};

    .list-group-item-heading {
      color: ${color.gray[20]};
    }

    &:hover,
    &:focus {
      color: ${color.global.linkHover};
      background-color: ${color.gray[80]};
    }
  }
`);

const ListGroupItem = forwardRef((props, ref) => {
  return <StyledListGroupItem ref={ref} {...props} />;
});

export default ListGroupItem;
