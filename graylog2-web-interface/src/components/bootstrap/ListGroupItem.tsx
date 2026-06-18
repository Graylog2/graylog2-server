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
import * as React from 'react';
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';
import { List } from '@mantine/core';

type StyledProps = {
  $active?: boolean;
  $disabled?: boolean;
  $bsStyle?: string;
  $isInteractive?: boolean;
};

const StyledListItem = styled(List.Item)<StyledProps>(
  ({ theme, $active, $disabled, $bsStyle, $isInteractive }) => css`
    background-color: ${theme.colors.global.contentBackground};
    border: 0;
    line-height: 1.25;
    padding: 5px 10px;

    .mantine-List-itemWrapper {
      display: flex;
    }

    .mantine-List-itemWrapper,
    .mantine-List-itemLabel {
      width: 100%;
    }

    &:not(:last-child) {
      border-bottom: 1px solid ${theme.colors.table.row.divider};
    }

    .list-group-item-heading {
      font-size: ${theme.fonts.size.h5};
    }

    .list-group-item-text {
      margin-bottom: 5px;
    }

    ${$isInteractive &&
    css`
      cursor: pointer;
      color: ${theme.colors.text.primary};

      .list-group-item-heading {
        color: ${theme.colors.variant.darkest.default};
      }

      &:hover:not([disabled]),
      &:focus:not([disabled]) {
        color: inherit;
        background-color: ${theme.utils.colorLevel(theme.colors.global.contentBackground, 10)};

        .list-group-item-heading {
          color: ${theme.utils.readableColor(theme.colors.variant.lightest.default)};
        }
      }
    `}

    ${$disabled &&
    css`
      pointer-events: none;
      color: ${theme.colors.text.disabled};
      background-color: ${theme.colors.variant.lightest.default};

      .list-group-item-heading {
        color: inherit;
      }

      .list-group-item-text {
        color: ${theme.colors.variant.default};
      }
    `}

    ${$active &&
    css`
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
    `}

    ${$bsStyle &&
    css`
      color: ${theme.utils.readableColor(theme.colors.variant.lighter[$bsStyle])};
      background-color: ${theme.colors.variant.lighter[$bsStyle]};

      ${$isInteractive &&
      css`
        &:hover,
        &:focus {
          color: ${theme.utils.readableColor(theme.colors.variant.lighter[$bsStyle])};
          background-color: ${theme.colors.variant.light[$bsStyle]};
        }

        ${$active &&
        css`
          color: ${theme.utils.readableColor(theme.colors.variant.light[$bsStyle])};
          background-color: ${theme.colors.variant.light[$bsStyle]};
          border-color: ${theme.colors.variant.light[$bsStyle]};
        `}
      `}
    `}
  `,
);

type Props = React.PropsWithChildren<{
  id?: string;
  active?: boolean;
  bsStyle?: string;
  className?: string;
  disabled?: boolean;
  header?: React.ReactNode;
  onClick?: () => void;
  onKeyDown?: React.KeyboardEventHandler;
  role?: 'listitem';
}>;

const ListGroupItem = (
  {
    active = undefined,
    bsStyle = undefined,
    children = undefined,
    className = undefined,
    disabled = undefined,
    header = undefined,
    id = undefined,
    onClick = undefined,
    onKeyDown = undefined,
    role = undefined,
  }: Props,
  ref: React.ForwardedRef<HTMLLIElement>,
) => {
  const isInteractive = !!onClick;

  return (
    <StyledListItem
      ref={ref}
      id={id}
      className={className}
      role={role}
      $active={active}
      $disabled={disabled}
      $bsStyle={bsStyle}
      $isInteractive={isInteractive}
      onClick={!disabled ? onClick : undefined}
      onKeyDown={onKeyDown}>
      {header && <div className="list-group-item-heading">{header}</div>}
      {header ? <p className="list-group-item-text">{children}</p> : children}
    </StyledListItem>
  );
};

export default forwardRef(ListGroupItem);
