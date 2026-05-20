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
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import styled, { useTheme } from 'styled-components';

import { FoundationItem, FoundationTable, PxLabel, StoryContainer, Token, UsageNote } from './shared';

// Root font size matching theme/constants ROOT_FONT_SIZE
const ROOT_FONT_SIZE = 16;
const remToPx = (rem: string): string => `${Math.round(parseFloat(rem) * ROOT_FONT_SIZE)}px`;

// ─── Font Families ─────────────────────────────────────────────────────────

const FamilyLabel = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  color: ${({ theme }) => theme.colors.text.primary};
  display: block;
  margin: ${({ theme }) => theme.spacings.xxs} 0 ${({ theme }) => theme.spacings.xxs};
`;

type FontFamilyKey = 'body' | 'navigation' | 'monospace';

type FamilyEntry = {
  key: FontFamilyKey;
  label: string;
  token: string;
  specimen: string;
  when: string;
  not: string;
};

const FONT_FAMILIES: FamilyEntry[] = [
  {
    key: 'body',
    label: 'Source Sans Pro — Body',
    token: 'theme.fonts.family.body',
    specimen: 'The quick brown fox jumps over the lazy dog. 0123456789',
    when: 'All UI copy by default: paragraphs, labels, form inputs, buttons, table cells, tooltips, descriptions. GlobalThemeStyles applies this globally — you rarely need to set it explicitly.',
    not: 'Headings h1/h2 (use navigation family) and code or log output (use monospace).',
  },
  {
    key: 'navigation',
    label: 'DM Sans — Navigation',
    token: 'theme.fonts.family.navigation',
    specimen: 'The quick brown fox jumps over the lazy dog. 0123456789',
    when: 'h1 and h2 headings only. GlobalThemeStyles applies this automatically — you do not need to set it on h1/h2 elements yourself.',
    not: 'Body text, nav links, or any heading below h2. DM Sans is intentionally reserved for the top two heading levels to create hierarchy.',
  },
  {
    key: 'monospace',
    label: 'Ubuntu Mono — Monospace',
    token: 'theme.fonts.family.monospace',
    specimen: 'The quick brown fox jumps over the lazy dog. 0123456789',
    when: 'All programmatic content: code blocks, inline code, log output, search queries, pipeline rules, configuration values, and technical identifiers.',
    not: 'Human-readable labels or descriptions, even if they reference technical concepts by name.',
  },
];

const FontFamiliesDoc = () => {
  const theme = useTheme();

  return (
    <StoryContainer>
      <h1 style={{ marginBottom: theme.spacings.lg }}>Font Families</h1>
      {FONT_FAMILIES.map(({ key, label, token, specimen, when, not }) => (
        <FoundationItem key={key}>
          <Token>{token}</Token>
          <p
            style={{
              fontFamily: (theme.fonts.family as Record<string, string>)[key],
              fontSize: '1.3rem',
              color: theme.colors.text.primary,
              margin: `${theme.spacings.xs} 0 0`,
              lineHeight: 1.35,
            }}>
            {specimen}
          </p>
          <FamilyLabel>{label}</FamilyLabel>
          <UsageNote>
            <strong>Use when: </strong>
            {when}
          </UsageNote>
          <UsageNote>
            <strong>Not for: </strong>
            {not}
          </UsageNote>
        </FoundationItem>
      ))}
    </StoryContainer>
  );
};

// ─── Type Scale ────────────────────────────────────────────────────────────

const ScaleGroup = styled.section`
  margin-bottom: ${({ theme }) => theme.spacings.xl};
`;

const GroupHeading = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.sm};
`;

const GroupNote = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.md};
  line-height: 1.5;
`;

type FontSizeKey =
  | 'h1'
  | 'h2'
  | 'h3'
  | 'h4'
  | 'h5'
  | 'h6'
  | 'large'
  | 'body'
  | 'small'
  | 'tiny'
  | 'navigation'
  | 'huge';

type ScaleEntry = {
  key: FontSizeKey;
  token: string;
  family?: FontFamilyKey;
  specimen: string;
  usage: string;
};

type ScaleGroupEntry = {
  label: string;
  note: string;
  items: ScaleEntry[];
};

const TYPE_SCALE_GROUPS: ScaleGroupEntry[] = [
  {
    label: 'Headings',
    note: 'Applied automatically by GlobalThemeStyles to h1–h6 HTML elements. h1 additionally inherits DM Sans from the navigation font family.',
    items: [
      {
        key: 'h1',
        token: 'theme.fonts.size.h1',
        family: 'navigation',
        specimen: 'Heading 1',
        usage: 'Used for all page titles.',
      },
      {
        key: 'h3',
        token: 'theme.fonts.size.h3',
        specimen: 'Heading 3',
        usage: 'Used for modal and drawer titles.',
      },
      {
        key: 'h4',
        token: 'theme.fonts.size.h4',
        specimen: 'Heading 4',
        usage: 'Used for card and section titles.',
      },
      {
        key: 'h5',
        token: 'theme.fonts.size.h5',
        specimen: 'Heading 5',
        usage: 'Used for section titles.',
      },
    ],
  },
  {
    label: 'Body',
    note: 'Source Sans Pro. Paragraph spacing is 8px for all body text.',
    items: [
      {
        key: 'body',
        token: 'theme.fonts.size.body',
        specimen: 'Body MD',
        usage: 'Typically used for infographics and messages.',
      },
      {
        key: 'small',
        token: 'theme.fonts.size.small',
        specimen: 'Body SM',
        usage: 'Our main font size for all content.',
      },
      {
        key: 'tiny',
        token: 'theme.fonts.size.tiny',
        specimen: 'Body XS',
        usage: 'Used sparingly for small elements such as badges and buttons. Not recommended for large bodies of text because its size can be hard to read.',
      },
    ],
  },
  {
    label: 'Monospace',
    note: 'Ubuntu Mono. Reserved for log messages, code snippets, and other system-related content. Its fixed-width design improves scannability and makes it easier to differentiate between characters.',
    items: [
      {
        key: 'body',
        token: 'theme.fonts.size.body',
        family: 'monospace',
        specimen: 'Body MD',
        usage: 'Typically used for reports.',
      },
      {
        key: 'small',
        token: 'theme.fonts.size.small',
        family: 'monospace',
        specimen: 'Body SM',
        usage: 'Our main font size for all system related content.',
      },
      {
        key: 'tiny',
        token: 'theme.fonts.size.tiny',
        family: 'monospace',
        specimen: 'Body XS',
        usage: 'Used sparingly. Not recommended for large bodies of text because its size can be hard to read.',
      },
    ],
  },
];

const TypeScaleDoc = () => {
  const theme = useTheme();
  const sizes = theme.fonts.size as Record<string, string>;
  const families = theme.fonts.family as Record<string, string>;

  const columns = [
    {
      header: 'Style',
      width: '200px',
      render: (row: ScaleEntry) => {
        const sizeValue = sizes[row.key] ?? '1rem';

        return (
          <span
            style={{
              fontFamily: families[row.family ?? 'body'],
              fontSize: sizeValue,
              color: theme.colors.text.primary,
              lineHeight: 1.3,
            }}>
            {row.specimen}
          </span>
        );
      },
    },
    {
      header: 'Variable',
      width: '260px',
      render: (row: ScaleEntry) => <Token>{row.token}</Token>,
    },
    {
      header: 'Usage',
      render: (row: ScaleEntry) => {
        const sizeValue = sizes[row.key] ?? '1rem';

        return (
          <>
            <PxLabel>
              {sizeValue} / {remToPx(sizeValue)}
            </PxLabel>
            <div>{row.usage}</div>
          </>
        );
      },
    },
  ];

  return (
    <StoryContainer>
      <h1 style={{ marginBottom: theme.spacings.lg }}>Type Scale</h1>
      {TYPE_SCALE_GROUPS.map((group) => (
        <ScaleGroup key={group.label}>
          <GroupHeading>{group.label}</GroupHeading>
          <GroupNote>{group.note}</GroupNote>
          <FoundationTable
            columns={columns}
            rows={group.items}
            keyBy={(row) => `${row.key}-${row.family ?? 'default'}`}
          />
        </ScaleGroup>
      ))}
    </StoryContainer>
  );
};

// ─── Text Colors ───────────────────────────────────────────────────────────

const Swatch = styled.div<{ $color: string }>`
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background-color: ${({ $color }) => $color};
  border: 1px solid rgba(128, 128, 128, 0.25);
  margin-bottom: ${({ theme }) => theme.spacings.xs};
  flex-shrink: 0;
`;

type ColorEntry = {
  token: string;
  getColor: (theme: any) => string;
  usage: string;
};

const TEXT_COLORS: ColorEntry[] = [
  {
    token: 'theme.colors.text.primary',
    getColor: (t) => t.colors.text.primary,
    usage: 'Primary text. This should be used for all text in the app.',
  },
  {
    token: 'theme.colors.text.secondary',
    getColor: (t) => t.colors.text.secondary,
    usage: 'Light theme secondary text. Used for most descriptions, charts, and table headers.',
  },
  {
    token: 'theme.colors.text.disabled',
    getColor: (t) => t.colors.text.disabled,
    usage: 'Light theme disabled text.',
  },
];

const TextColorsDoc = () => {
  const theme = useTheme();

  const columns = [
    {
      header: 'Style',
      width: '220px',
      render: (row: ColorEntry) => <Swatch $color={row.getColor(theme)} />,
    },
    {
      header: 'Variable',
      width: '260px',
      render: (row: ColorEntry) => <Token>{row.token}</Token>,
    },
    {
      header: 'Usage',
      render: (row: ColorEntry) => row.usage,
    },
  ];

  return (
    <StoryContainer>
      <h1 style={{ marginBottom: theme.spacings.lg }}>Text Colors</h1>
      <FoundationTable columns={columns} rows={TEXT_COLORS} keyBy={(row) => row.token} />
    </StoryContainer>
  );
};

// ─── Meta & Stories ────────────────────────────────────────────────────────

const meta: Meta = {
  title: 'Foundation/Typography',
  parameters: {
    layout: 'padded',
    docs: {
      description: {
        component:
          'Typography tokens from `theme.fonts` and `theme.colors.text`. Always use these tokens — never hardcode font families, sizes, or colors. Toggle light/dark mode in the toolbar to preview both themes.',
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const FontFamilies: Story = {
  render: FontFamiliesDoc,
  parameters: {
    docs: {
      description: {
        story:
          'Three font families cover every content type. The choice is dictated by the nature of the content, not visual preference.',
      },
    },
  },
};

export const TypeScale: Story = {
  render: TypeScaleDoc,
  parameters: {
    docs: {
      description: {
        story:
          'Sizes are expressed in `rem` relative to a 16 px root and organized by semantic role. Use the token name — never hardcode pixel values.',
      },
    },
  },
};

export const TextColors: Story = {
  render: TextColorsDoc,
  parameters: {
    docs: {
      description: {
        story:
          'Text colors adapt to light and dark mode automatically. Toggle the mode in the Storybook toolbar to preview both. Never use hardcoded hex values for text.',
      },
    },
  },
};
