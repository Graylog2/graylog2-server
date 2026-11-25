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
import React from 'react';
import styled from 'styled-components';

import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from 'theme/sawmill';
import SawmillMantine from 'theme/sawmill/mantine/Sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';

import { ColorSwatch, FoundationTable, HexValue, StoryContainer, Token } from './shared';
import { PALETTE_NAMES } from './colorPalette';
import { BORDER_TOKENS } from './lumaTokens';

// ─── Token groups ──────────────────────────────────────────────────────────
// Edit this list to add, remove or regroup tokens. `path` is the path below `theme.colors`.
// Light and dark values are read from the real Graylog theme, so they never go stale.

type TokenEntry = {
  /** Path below `theme.colors`, for tokens that exist in the Graylog theme. */
  path?: string;
  /** Luma token name and values, for tokens not in the theme yet (see lumaTokens.ts). */
  name?: string;
  values?: { light: string; dark: string };
  usage: string;
};

const GROUPS = {
  text: [
    { path: 'text.primary', usage: 'Default body and heading text.' },
    { path: 'text.secondary', usage: 'Supporting text: descriptions, help text, metadata, table headers.' },
    { path: 'text.disabled', usage: 'Text in disabled controls.' },
    {
      path: 'global.textAlt',
      usage: 'Text on dark or colored surfaces where the primary text color would not contrast.',
    },
    { path: 'input.placeholder', usage: 'Placeholder text inside form inputs.' },
    { path: 'global.link', usage: 'Inline links.' },
    { path: 'global.linkHover', usage: 'Inline links on hover.' },
  ],
  surfaces: [
    { path: 'global.background', usage: 'Page background behind all content.' },
    { path: 'global.contentBackground', usage: 'Main content areas: panels, cards, tables and modals.' },
    { path: 'global.navigationBackground', usage: 'Top navigation bar.' },
    { path: 'background.secondaryNav', usage: 'Secondary navigation.' },
    { path: 'section.filled.background', usage: 'Filled sections that group content inside a page.' },
    { path: 'cards.background', usage: 'Card backgrounds.' },
    { path: 'input.background', usage: 'Form input backgrounds.' },
    { path: 'table.row.backgroundStriped', usage: 'Alternate rows in striped tables.' },
    { path: 'table.row.backgroundHover', usage: 'Table rows on hover.' },
  ],
  borders: [
    {
      name: 'divider',
      values: BORDER_TOKENS.divider,
      usage: 'All borders: section header lines, tables, cards, inputs and swatches.',
    },
    {
      name: 'dividerSecondary',
      values: BORDER_TOKENS.dividerSecondary,
      usage: 'Stronger, fully opaque separators where divider is too subtle.',
    },
  ],
} satisfies Record<string, TokenEntry[]>;

// ─── Theme values ──────────────────────────────────────────────────────────

const buildColors = (scheme: typeof COLOR_SCHEME_LIGHT | typeof COLOR_SCHEME_DARK) =>
  SawmillSC(SawmillMantine({ colorScheme: scheme })).colors;

const THEME_COLORS = { light: buildColors(COLOR_SCHEME_LIGHT), dark: buildColors(COLOR_SCHEME_DARK) };

const lookup = (colors: unknown, path: string): string | undefined =>
  path.split('.').reduce<any>((value, key) => (value == null ? undefined : value[key]), colors);

// Matches a value to a Luma palette color. 8-digit hex values are a palette color plus opacity.
const paletteName = (value: string | undefined) => {
  if (!value || !value.startsWith('#')) return undefined;

  let hex = value.toUpperCase();

  if (hex.length === 4) hex = `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`;

  const base = PALETTE_NAMES[hex.slice(0, 7)];

  if (!base) return undefined;
  if (hex.length === 9) return `${base} @ ${Math.round((parseInt(hex.slice(7), 16) / 255) * 100)}%`;

  return base;
};

// ─── Layout ────────────────────────────────────────────────────────────────

const ValueCell = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const PaletteLabel = styled.span<{ $missing?: boolean }>`
  display: block;
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ $missing, theme }) => ($missing ? theme.colors.variant.dark.danger : theme.colors.text.secondary)};
`;

const ColorValue = ({ value }: { value: string | undefined }) => {
  if (!value) return <HexValue>—</HexValue>;

  const name = paletteName(value);

  return (
    <ValueCell>
      <ColorSwatch color={value} />
      <div>
        <HexValue>{value}</HexValue>
        <PaletteLabel $missing={!name}>{name ?? 'Not in Luma palette'}</PaletteLabel>
      </div>
    </ValueCell>
  );
};

type Row = TokenEntry & { light?: string; dark?: string };

const COLUMNS = [
  {
    header: 'Token',
    width: '300px',
    render: (r: Row) => <Token>{r.path ? `theme.colors.${r.path}` : r.name}</Token>,
  },
  { header: 'Light', width: '190px', render: (r: Row) => <ColorValue value={r.light} /> },
  { header: 'Dark', width: '190px', render: (r: Row) => <ColorValue value={r.dark} /> },
  { header: 'Usage', render: (r: Row) => r.usage },
];

export const TokenGroupDoc = ({ group }: { group: keyof typeof GROUPS }) => {
  const rows: Row[] = GROUPS[group].map((token: TokenEntry) => ({
    ...token,
    light: token.values?.light ?? lookup(THEME_COLORS.light, token.path),
    dark: token.values?.dark ?? lookup(THEME_COLORS.dark, token.path),
  }));

  return (
    <StoryContainer>
      <FoundationTable columns={COLUMNS} rows={rows} keyBy={(r) => r.path ?? r.name} />
    </StoryContainer>
  );
};
