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

import { FoundationTable, PxLabel, StoryContainer, Token } from './shared';

// Root font size matching theme/constants ROOT_FONT_SIZE
const ROOT_FONT_SIZE = 16;
const remToPx = (rem: string): string => `${Math.round(parseFloat(rem) * ROOT_FONT_SIZE)}px`;

// ─── Font Families ─────────────────────────────────────────────────────────

type FontFamilyKey = 'body' | 'navigation' | 'monospace';

type FamilyEntry = {
  key: FontFamilyKey;
  label: string;
  token: string;
  when: string;
};

const FONT_FAMILIES: FamilyEntry[] = [
  {
    key: 'body',
    label: 'Body — Source Sans Pro',
    token: 'theme.fonts.family.body',
    when: 'All UI copy by default: paragraphs, labels, form inputs, buttons, table cells, tooltips, descriptions. GlobalThemeStyles applies this globally — you rarely need to set it explicitly.',
  },
  {
    key: 'navigation',
    label: 'Navigation — DM Sans',
    token: 'theme.fonts.family.navigation',
    when: 'h1 and h2 headings only. GlobalThemeStyles applies this automatically — you do not need to set it on h1/h2 elements yourself.',
  },
  {
    key: 'monospace',
    label: 'Monospace — Ubuntu Mono',
    token: 'theme.fonts.family.monospace',
    when: 'All programmatic content: code blocks, inline code, log output, search queries, pipeline rules, configuration values, and technical identifiers.',
  },
];

const FontFamiliesDoc = () => {
  const theme = useTheme();
  const families = theme.fonts.family as Record<string, string>;

  const columns = [
    {
      header: 'Style',
      width: '320px',
      render: (row: FamilyEntry) => (
        <p
          style={{
            fontFamily: families[row.key],
            fontSize: '1.1rem',
            color: theme.colors.text.primary,
            margin: 0,
            lineHeight: 1.5,
          }}>
          {row.label}
          <br />
          0123456789
        </p>
      ),
    },
    {
      header: 'Variable',
      width: '240px',
      render: (row: FamilyEntry) => <Token>{row.token}</Token>,
    },
    {
      header: 'Usage',
      render: (row: FamilyEntry) => row.when,
    },
  ];

  return (
    <StoryContainer>
      <h2 style={{ marginBottom: theme.spacings.lg }}>Font Families</h2>
      <FoundationTable columns={columns} rows={FONT_FAMILIES} keyBy={(row) => row.key} />
    </StoryContainer>
  );
};

// ─── Type Scale ────────────────────────────────────────────────────────────

const ScaleGroup = styled.section`
  margin-bottom: ${({ theme }) => theme.spacings.xl};

  &:last-child {
    margin-bottom: 0;
  }
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
        key: 'h2',
        token: 'theme.fonts.size.h2',
        family: 'navigation',
        specimen: 'Heading 2',
        usage: 'Major sections within a page.',
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
      {
        key: 'h6',
        token: 'theme.fonts.size.h6',
        specimen: 'Heading 6',
        usage: 'Avoid if possible. A bold body-sized label is usually more appropriate.',
      },
    ],
  },
  {
    label: 'Body',
    note: 'Source Sans Pro. Paragraph spacing is 8px for all body text.',
    items: [
      {
        key: 'huge',
        token: 'theme.fonts.size.huge',
        specimen: 'Huge',
        usage: 'Large dashboard metric values or hero numbers.',
      },
      {
        key: 'large',
        token: 'theme.fonts.size.large',
        specimen: 'Large',
        usage: 'Lead paragraph or introductory sentence that needs a visual step above regular body without being a heading.',
      },
      {
        key: 'body',
        token: 'theme.fonts.size.body',
        specimen: 'Body',
        usage: 'Typically used for infographics and messages.',
      },
      {
        key: 'small',
        token: 'theme.fonts.size.small',
        specimen: 'Small',
        usage: 'Our main font size for all content.',
      },
      {
        key: 'tiny',
        token: 'theme.fonts.size.tiny',
        specimen: 'Tiny',
        usage: 'Used sparingly for small elements such as badges and buttons. Not recommended for large bodies of text because its size can be hard to read.',
      },
    ],
  },
  {
    label: 'Navigation',
    note: 'Used exclusively for navigation links. Applied by navigation components — do not set manually in other contexts.',
    items: [
      {
        key: 'navigation',
        token: 'theme.fonts.size.navigation',
        specimen: 'Navigation',
        usage: 'Link and dropdown labels in the navigation. Applied by navigation components — do not set manually.',
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
      <h2 style={{ marginBottom: theme.spacings.lg }}>Type Scale</h2>
      {TYPE_SCALE_GROUPS.map((group) => (
        <ScaleGroup key={group.label}>
          <h3>{group.label}</h3>
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
      <h2 style={{ marginBottom: theme.spacings.lg }}>Text Colors</h2>
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

const TypographyDoc = () => {
  const theme = useTheme();

  return (
    <>
      <StoryContainer>
        <h1>Typography</h1>
        <p style={{ color: theme.colors.text.secondary, lineHeight: 1.6 }}>
          Typography in our design system ensures clarity, consistency, and accessibility across all
          user interfaces. We use a dual-typeface approach to distinguish between general interface
          content and technical or system-specific elements.
        </p>
      </StoryContainer>
      <FontFamiliesDoc />
      <TypeScaleDoc />
      <TextColorsDoc />
    </>
  );
};

export const Typography: Story = {
  render: TypographyDoc,
};
