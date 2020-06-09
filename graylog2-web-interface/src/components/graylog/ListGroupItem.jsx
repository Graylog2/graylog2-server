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
  border-color: ${theme.colors.gray[80]};

  .list-group-item-heading {
    font-weight: bold;
  }

  &.disabled,
  &.disabled:hover,
  &.disabled:focus {
    color: ${theme.colors.gray[60]};
    background-color: ${theme.colors.gray[90]};

    .list-group-item-heading {
      color: inherit;
      font-weight: bold;
    }

    .list-group-item-text {
      color: ${theme.colors.gray[60]};
    }
  }

  &.active,
  &.active:hover,
  &.active:focus {
    color: ${theme.colors.gray[100]};
    background-color: ${theme.colors.variant.light.primary};
    border-color: ${theme.colors.variant.light.primary};
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

  a&,
  button& {
    color: ${theme.colors.global.link};

    .list-group-item-heading {
      color: ${theme.colors.gray[20]};
      font-weight: bold;
    }

    &:hover:not(.disabled),
    &:focus:not(.disabled) {
      background-color: ${theme.colors.gray[40]};
      color: ${theme.utils.readableColor(theme.colors.gray[40])};

      &.active {
        color: ${theme.utils.readableColor(theme.colors.variant.primary)};
        border-color: ${theme.colors.variant.primary};
        background-color: ${theme.colors.variant.primary};
      }

      .list-group-item-heading {
        color: ${theme.utils.readableColor(theme.colors.gray[40])};
      }
    }
  }

  ${variantStyles};
`);

const ListGroupItem = forwardRef((props, ref) => {
  return <StyledListGroupItem {...props} ref={ref} />;
});


export default ListGroupItem;
