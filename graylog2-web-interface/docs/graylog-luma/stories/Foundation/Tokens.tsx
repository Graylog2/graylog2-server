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
import React, { useMemo } from 'react';
import styled from 'styled-components';

import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from 'theme/sawmill';
import SawmillMantine from 'theme/sawmill/mantine/Sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';

import { FoundationTable, StoryContainer, Token } from './shared';
import { PALETTE_NAMES } from './colorPalette';
import { BORDER_TOKENS, dividerColor } from './lumaTokens';

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

type TokenGroup = {
  title: string;
  intro?: string;
  tokens: TokenEntry[];
};

const GROUPS: TokenGroup[] = [
  {
    title: 'Text',
    intro: 'Colors for text and inline links.',
    tokens: [
      { path: 'text.primary', usage: 'Default body and heading text.' },
      { path: 'text.secondary', usage: 'Supporting text: descriptions, help text, metadata, table headers.' },
      { path: 'text.disabled', usage: 'Text in disabled controls.' },
      { path: 'global.textAlt', usage: 'Text on dark or colored surfaces where the primary text color would not contrast.' },
      { path: 'input.placeholder', usage: 'Placeholder text inside form inputs.' },
      { path: 'global.link', usage: 'Inline links.' },
      { path: 'global.linkHover', usage: 'Inline links on hover.' },
    ],
  },
  {
    title: 'Surfaces',
    intro: 'Background colors for the page and the containers on it, from back to front.',
    tokens: [
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
  },
  {
    title: 'Borders',
    intro:
      'Border colors. These Luma tokens are not in the Graylog theme yet, so Storybook builds them from the Luma palette.',
    tokens: [
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
  },
];

// ─── Theme values ──────────────────────────────────────────────────────────

const buildColors = (scheme: typeof COLOR_SCHEME_LIGHT | typeof COLOR_SCHEME_DARK) =>
  SawmillSC(SawmillMantine({ colorScheme: scheme })).colors;

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

// Border and spacing above come from the global H2 rule in .storybook/DocsContainer.tsx.
const GroupTitle = styled.h2`
  margin-bottom: ${({ theme }) => theme.spacings.xs};
`;

const Muted = styled.p`
  margin: ${({ theme }) => `${theme.spacings.xxs} 0 ${theme.spacings.sm}`};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const ValueCell = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

// Checkerboard behind the swatch so semi-transparent colors are visible.
const SwatchChip = styled.div<{ $color: string }>`
  flex-shrink: 0;
  width: 32px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  background:
    linear-gradient(${({ $color }) => $color}, ${({ $color }) => $color}),
    repeating-conic-gradient(#ccc 0% 25%, #fff 0% 50%) 0 0 / 8px 8px;
`;

const Hex = styled.span`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: 12px;
`;

const PaletteLabel = styled.span<{ $missing?: boolean }>`
  display: block;
  font-size: 11px;
  color: ${({ $missing, theme }) => ($missing ? '#E74342' : theme.colors.text.secondary)};
`;

const ColorValue = ({ value }: { value: string | undefined }) => {
  if (!value) return <Hex>—</Hex>;

  const name = paletteName(value);

  return (
    <ValueCell>
      <SwatchChip $color={value} />
      <div>
        <Hex>{value}</Hex>
        <PaletteLabel $missing={!name}>{name ?? 'Not in Luma palette'}</PaletteLabel>
      </div>
    </ValueCell>
  );
};

type Row = TokenEntry & { light?: string; dark?: string };

export const TokensDoc = () => {
  const { light, dark } = useMemo(
    () => ({ light: buildColors(COLOR_SCHEME_LIGHT), dark: buildColors(COLOR_SCHEME_DARK) }),
    [],
  );

  return (
    <StoryContainer>
      {GROUPS.map((group) => {
        const rows: Row[] = group.tokens.map((token) => ({
          ...token,
          light: token.values?.light ?? lookup(light, token.path),
          dark: token.values?.dark ?? lookup(dark, token.path),
        }));

        return (
          <section key={group.title}>
            <GroupTitle>{group.title}</GroupTitle>
            {group.intro && <Muted>{group.intro}</Muted>}
            <FoundationTable
              columns={[
                { header: 'Token', width: '300px', render: (r: Row) => <Token>{r.path ? `theme.colors.${r.path}` : r.name}</Token> },
                { header: 'Light', width: '190px', render: (r: Row) => <ColorValue value={r.light} /> },
                { header: 'Dark', width: '190px', render: (r: Row) => <ColorValue value={r.dark} /> },
                { header: 'Usage', render: (r: Row) => r.usage },
              ]}
              rows={rows}
              keyBy={(r) => r.path ?? r.name}
            />
          </section>
        );
      })}
    </StoryContainer>
  );
};
