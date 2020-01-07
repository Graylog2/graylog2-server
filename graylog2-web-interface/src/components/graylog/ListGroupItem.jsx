import { memo } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ListGroupItem as BootstrapListGroupItem } from 'react-bootstrap';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const listGroupItemStyles = (hex, variant) => css(({ theme }) => {
  const backgroundColor = util.colorLevel(theme.color.variant[variant], -9);
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
          background-color: ${theme.color.variant.light[variant]};
        }

        &.active,
        &.active:hover,
        &.active:focus {
          color: ${util.readableColor(theme.color.variant.light[variant])};
          background-color: ${theme.color.variant.light[variant]};
          border-color: ${theme.color.variant.light[variant]};
        }
      }
    }
  `;
});

const ListGroupItem = memo(styled(BootstrapListGroupItem)(({ theme }) => {
  return css`
    ${bsStyleThemeVariant(listGroupItemStyles, {}, ['success', 'info', 'warning', 'danger'])};

    background-color: ${theme.color.gray[90]};
    border-color: ${theme.color.gray[80]};

    &&.disabled,
    &&.disabled:hover,
    &&.disabled:focus {
      color: ${theme.color.gray[60]};
      background-color: ${theme.color.gray[90]};

      .list-group-item-text {
        color: ${theme.color.gray[60]};
      }
    }

    &.active,
    &.active:hover,
    &.active:focus {
      color: ${theme.color.gray[100]};
      background-color: ${theme.color.variant.primary};
      border-color: ${theme.color.variant.light.primary};

      .list-group-item-text {
        color: ${theme.color.variant.light.primary};
      }
    }

    a&,
    button& {
      color: ${theme.color.global.link};

      .list-group-item-heading {
        color: ${theme.color.gray[20]};
      }

      &:hover,
      &:focus {
        color: ${theme.color.global.linkHover};
        background-color: ${theme.color.gray[80]};
      }
    }
  `;
}));

export default ListGroupItem;
