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
import { Tree } from '@mantine/core';
import styled, { css } from 'styled-components';

const PANEL_HEIGHT = '640px';
const TREE_PANE_MIN_WIDTH = '300px';
const TREE_PANE_MAX_WIDTH = '340px';
const MOBILE_TREE_MAX_HEIGHT = '420px';
const BORDER_RADIUS = '8px';

export const ModuleContent = styled.div(
  ({ theme }) => css`
    padding-top: ${theme.spacings.md};

    &::before {
      content: '';
      display: block;
      border-top: 1px solid ${theme.colors.variant.lighter.default};
    }
  `,
);

export const ModuleLayout = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: minmax(${TREE_PANE_MIN_WIDTH}, ${TREE_PANE_MAX_WIDTH}) minmax(0, 1fr);
    height: ${PANEL_HEIGHT};

    @media (max-width: ${theme.breakpoints.max.sm}) {
      grid-template-columns: 1fr;
      height: auto;
    }
  `,
);

export const TreePane = styled.div(
  ({ theme }) => css`
    padding-top: ${theme.spacings.md};
    padding-right: ${theme.spacings.md};
    margin-right: ${theme.spacings.md};
    border-right: 1px solid ${theme.colors.variant.lighter.default};
    max-height: ${PANEL_HEIGHT};
    overflow: auto;

    @media (max-width: ${theme.breakpoints.max.sm}) {
      padding-top: ${theme.spacings.md};
      padding-right: 0;
      margin-right: 0;
      padding-bottom: ${theme.spacings.md};
      margin-bottom: ${theme.spacings.md};
      border-right: 0;
      border-bottom: 1px solid ${theme.colors.variant.lighter.default};
      max-height: ${MOBILE_TREE_MAX_HEIGHT};
    }
  `,
);

export const DetailsPane = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
    padding-top: ${theme.spacings.md};
    overflow-y: auto;
    min-height: 0;
  `,
);

export const InterpretationPane = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
    padding-top: ${theme.spacings.md};
    overflow-y: auto;
    min-height: 0;
  `,
);

export const InterpretationTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
    line-height: 1.2;
  `,
);

export const LegendList = styled.ul(
  ({ theme }) => css`
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.sm};
  `,
);

export const LegendItem = styled.li(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    line-height: 1.5;
  `,
);

export const LegendText = styled.span(
  () => css`
    min-width: 0;

    strong {
      font-weight: 600;
    }
  `,
);

export const StyledTree = styled(Tree)(
  ({ theme }) => css`
    margin: 0;
    padding: 0;

    .mantine-Tree-node {
      list-style: none;
      border-radius: ${BORDER_RADIUS};
      outline: none;
    }

    .mantine-Tree-node:focus-visible {
      outline: 2px solid ${theme.colors.variant.info};
      outline-offset: 2px;
    }

    .mantine-Tree-subtree {
      margin: 0;
      padding-top: ${theme.spacings.xxs};
      padding-bottom: ${theme.spacings.xxs};
    }
  `,
);

export const TreeRow = styled.div<{ $selected: boolean }>(
  ({ $selected, theme }) => css`
    width: 100%;
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    padding-top: ${theme.spacings.xxs};
    padding-right: ${theme.spacings.xs};
    padding-bottom: ${theme.spacings.xxs};
    border-radius: ${BORDER_RADIUS};
    min-height: 34px;
    cursor: pointer;
    color: ${theme.colors.text.primary};
    background-color: ${$selected ? theme.colors.variant.lightest.default : 'transparent'};
    box-shadow: ${$selected ? `inset 0 0 0 1px ${theme.colors.variant.lighter.default}` : 'none'};
    transition:
      background-color 120ms ease,
      box-shadow 120ms ease;

    &:hover {
      background-color: ${$selected ? theme.colors.variant.lightest.default : theme.colors.table.row.backgroundHover};
    }
  `,
);

export const TreeLabel = styled.span<{ $emphasized: boolean }>(
  ({ $emphasized, theme }) => css`
    min-width: 0;
    font-size: ${theme.fonts.size.small};
    font-weight: ${$emphasized ? 600 : 400};
    line-height: 1.3;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  `,
);

export const TreeCountSuffix = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.tiny};
    line-height: 1.3;
    flex-shrink: 0;
  `,
);

export const ChevronSlot = styled.span(
  ({ theme }) => css`
    width: 18px;
    height: 18px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: ${theme.colors.text.secondary};
    flex-shrink: 0;
  `,
);

export const Breadcrumbs = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
    line-height: 1.4;
  `,
);

export const DetailsTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h2};
    line-height: 1.15;
  `,
);

export const StatusSummary = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

export const BodyText = styled.p(
  ({ theme }) => css`
    margin: 0;
    color: ${theme.colors.text.primary};
    line-height: 1.55;
  `,
);

export const DetailSection = styled.section(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    padding-top: ${theme.spacings.sm};
    border-top: 1px solid ${theme.colors.variant.lighter.default};

    h4 {
      margin: 0;
      font-size: ${theme.fonts.size.small};
      font-weight: 600;
      line-height: 1.3;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: ${theme.colors.text.secondary};
    }
  `,
);

export const ChildList = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
  `,
);

export const ChildButton = styled.button`
  ${({ theme }) => css`
    width: 100%;
    border: 1px solid ${theme.colors.variant.lighter.default};
    background-color: ${theme.colors.global.contentBackground};
    border-radius: ${BORDER_RADIUS};
    padding: ${theme.spacings.sm};
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: ${theme.spacings.sm};
    text-align: left;
    cursor: pointer;

    &:hover {
      background-color: ${theme.colors.table.row.backgroundHover};
    }

    &:focus-visible {
      outline: 2px solid ${theme.colors.variant.info};
      outline-offset: 2px;
    }
  `}
`;

export const ChildButtonMeta = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: flex-start;
    gap: ${theme.spacings.sm};
    min-width: 0;
  `,
);

export const ChildButtonText = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    min-width: 0;

    strong {
      font-size: ${theme.fonts.size.small};
      line-height: 1.3;
    }

    span {
      color: ${theme.colors.text.secondary};
      font-size: ${theme.fonts.size.small};
      line-height: 1.3;
    }
  `,
);

export const ChildCountSuffix = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-weight: 400;
    font-size: ${theme.fonts.size.tiny};
  `,
);

export const MessageBlock = styled.pre(
  ({ theme }) => css`
    margin: 0;
    padding: ${theme.spacings.sm};
    border-radius: ${BORDER_RADIUS};
    background-color: ${theme.colors.variant.lightest.default};
    color: ${theme.colors.text.primary};
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  `,
);

export const CauseList = styled.ul(
  ({ theme }) => css`
    margin: 0;
    padding-left: ${theme.spacings.md};
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};

    li {
      line-height: 1.5;
      color: ${theme.colors.text.primary};
    }
  `,
);
