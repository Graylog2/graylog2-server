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
    border: none;
    font: inherit;
    text-align: left;
    width: 100%;
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

type ItemBodyProps = StyledProps & {
  children: React.ReactNode;
  className?: string;
  disabled?: boolean;
  href?: string;
  onClick?: () => void;
  onKeyDown?: React.KeyboardEventHandler;
};

const ItemBody = ({ href = undefined, disabled = undefined, onClick = undefined, onKeyDown = undefined, children, ...styledProps }: ItemBodyProps) => {
  if (href) {
    return <InnerContainer as="a" href={href} {...styledProps}>{children}</InnerContainer>;
  }

  if (onClick) {
    return (
      <InnerContainer as="button" type="button" disabled={disabled} onClick={onClick} onKeyDown={onKeyDown} {...styledProps}>
        {children}
      </InnerContainer>
    );
  }

  return <InnerContainer {...styledProps}>{children}</InnerContainer>;
};

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
  }: Props,
  ref: React.ForwardedRef<HTMLLIElement>,
) => {
  const isInteractive = !!(onClick || href);

  const content = (
    <>
      {header && <div className="list-group-item-heading">{header}</div>}
      {header ? <p className="list-group-item-text">{children}</p> : children}
    </>
  );

  return (
    <StyledItem ref={ref} id={id}>
      <ItemBody
        href={href}
        disabled={disabled}
        onClick={onClick}
        onKeyDown={onKeyDown}
        className={className}
        $active={active}
        $disabled={disabled}
        $bsStyle={bsStyle}
        $isInteractive={isInteractive}>
        {content}
      </ItemBody>
    </StyledItem>
  );
};

export default forwardRef(ListGroupItem);
