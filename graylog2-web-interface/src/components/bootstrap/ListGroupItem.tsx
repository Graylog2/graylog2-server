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

type StyledProps = {
  $active?: boolean;
  $disabled?: boolean;
  $bsStyle?: string;
  $isInteractive?: boolean;
};

const StyledItem = styled.li(
  ({ theme }) => css`
    &:not(:last-child) {
      border-bottom: 1px solid ${theme.colors.table.row.divider};
    }
  `,
);

const InnerContainer = styled.div<StyledProps>(
  ({ theme, $active, $disabled, $bsStyle, $isInteractive }) => css`
    display: flex;
    padding: 5px 10px;
    background-color: ${theme.colors.global.contentBackground};
    line-height: 1.25;
    color: ${theme.colors.text.primary};
    text-decoration: none;

    .list-group-item-heading {
      font-size: ${theme.fonts.size.h5};
    }

    .list-group-item-text {
      margin-bottom: 5px;
    }

    ${$isInteractive &&
    css`
      cursor: pointer;

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
  href?: string;
  onClick?: () => void;
  onKeyDown?: React.KeyboardEventHandler;
  role?: 'listitem' | 'button';
}>;

const ListGroupItem = (
  {
    active = undefined,
    bsStyle = undefined,
    children = undefined,
    className = undefined,
    disabled = undefined,
    header = undefined,
    href = undefined,
    id = undefined,
    onClick = undefined,
    onKeyDown = undefined,
    role = undefined,
  }: Props,
  ref: React.ForwardedRef<HTMLLIElement>,
) => {
  const isLink = !!href;
  const isInteractive = !!(onClick || href);
  const effectiveRole = role ?? (isInteractive && !isLink ? 'button' : undefined);

  const content = (
    <>
      {header && <div className="list-group-item-heading">{header}</div>}
      {header ? <p className="list-group-item-text">{children}</p> : children}
    </>
  );

  const sharedInnerProps = {
    className,
    $active: active,
    $disabled: disabled,
    $bsStyle: bsStyle,
    $isInteractive: isInteractive,
  };

  return (
    <StyledItem ref={ref} id={id}>
      {href ? (
        <InnerContainer as="a" href={href} {...sharedInnerProps}>
          {content}
        </InnerContainer>
      ) : (
        <InnerContainer
          {...sharedInnerProps}
          role={effectiveRole}
          tabIndex={isInteractive && !disabled ? 0 : undefined}
          onClick={!disabled ? onClick : undefined}
          onKeyDown={onKeyDown}>
          {content}
        </InnerContainer>
      )}
    </StyledItem>
  );
};

export default forwardRef(ListGroupItem);
