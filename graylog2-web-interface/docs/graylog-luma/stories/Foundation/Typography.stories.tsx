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

// Root font size matching theme/constants ROOT_FONT_SIZE
const ROOT_FONT_SIZE = 16;
const remToPx = (rem: string): string => `${Math.round(parseFloat(rem) * ROOT_FONT_SIZE)}px`;

// ─── Shared primitives ─────────────────────────────────────────────────────

const Token = styled.code`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  background: rgba(128, 128, 128, 0.12);
  padding: 1px 6px;
  border-radius: 3px;
`;

const UsageNote = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 4px 0 0;
  line-height: 1.5;
`;

const Strong = styled.strong`
  color: ${({ theme }) => theme.colors.text.primary};
`;

// ─── Font Families ─────────────────────────────────────────────────────────

const FamilySection = styled.div`
  padding: 24px 0;

  & + & {
    border-top: 1px solid rgba(128, 128, 128, 0.2);
  }
`;

const FamilyLabel = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  color: ${({ theme }) => theme.colors.text.primary};
  display: block;
  margin: 10px 0 4px;
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
    <div>
      {FONT_FAMILIES.map(({ key, label, token, specimen, when, not }) => (
        <FamilySection key={key}>
          <Token>{token}</Token>
          <p
            style={{
              fontFamily: (theme.fonts.family as Record<string, string>)[key],
              fontSize: '1.3rem',
              color: theme.colors.text.primary,
              margin: '12px 0 2px',
              lineHeight: 1.35,
            }}>
            {specimen}
          </p>
          <FamilyLabel>{label}</FamilyLabel>
          <UsageNote>
            <Strong>Use when: </Strong>
            {when}
          </UsageNote>
          <UsageNote>
            <Strong>Not for: </Strong>
            {not}
          </UsageNote>
        </FamilySection>
      ))}
    </div>
  );
};

// ─── Type Scale ────────────────────────────────────────────────────────────

const ScaleGroup = styled.section`
  margin-bottom: 40px;
`;

const GroupHeading = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 8px;
`;

const GroupNote = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 16px;
  line-height: 1.5;
`;

const ScaleItem = styled.div`
  padding: 16px 0;

  & + & {
    border-top: 1px solid rgba(128, 128, 128, 0.15);
  }
`;

const ScaleMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
`;

const PxLabel = styled.span`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
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
  when: string;
};

type ScaleGroupEntry = {
  label: string;
  note: string;
  items: ScaleEntry[];
};

const TYPE_SCALE_GROUPS: ScaleGroupEntry[] = [
  {
    label: 'Headings',
    note: 'Applied automatically by GlobalThemeStyles to h1–h6 HTML elements. h1 and h2 additionally inherit DM Sans from the navigation font family.',
    items: [
      {
        key: 'h1',
        token: 'theme.fonts.size.h1',
        family: 'navigation',
        specimen: 'Page Title',
        when: 'One per view. The primary title at the top of a page or full-screen dialog.',
      },
      {
        key: 'h2',
        token: 'theme.fonts.size.h2',
        family: 'navigation',
        specimen: 'Section Title',
        when: 'Major sections within a page — e.g. "General settings", "Advanced options".',
      },
      {
        key: 'h3',
        token: 'theme.fonts.size.h3',
        specimen: 'Sub-section Heading',
        when: 'Sub-sections within an h2 area, or card and panel titles.',
      },
      {
        key: 'h4',
        token: 'theme.fonts.size.h4',
        specimen: 'Group Label',
        when: 'Grouped settings or widget titles within a panel.',
      },
      {
        key: 'h5',
        token: 'theme.fonts.size.h5',
        specimen: 'Minor Heading',
        when: 'Rarely needed. Prefer h3/h4. Use for fine-grained labeling when more than three heading levels are unavoidable.',
      },
      {
        key: 'h6',
        token: 'theme.fonts.size.h6',
        specimen: 'Smallest Heading',
        when: 'Avoid if possible. A bold body-sized label is usually more appropriate.',
      },
    ],
  },
  {
    label: 'Body',
    note: 'Use these for all non-heading text. Default is body — only deviate when hierarchy requires it.',
    items: [
      {
        key: 'huge',
        token: 'theme.fonts.size.huge',
        specimen: 'Display number',
        when: 'Large dashboard metric values or hero numbers. Use only in data-heavy views where a dominant number is the focal point.',
      },
      {
        key: 'large',
        token: 'theme.fonts.size.large',
        specimen: 'Slightly emphasized body text',
        when: 'Lead paragraph or introductory sentence that needs a visual step above regular body without being a heading.',
      },
      {
        key: 'body',
        token: 'theme.fonts.size.body',
        specimen: 'Regular body text — the default for all UI copy and labels',
        when: 'Everything: paragraphs, labels, form inputs, button labels, table cells. This is the baseline — use it unless you have a specific reason to deviate.',
      },
      {
        key: 'small',
        token: 'theme.fonts.size.small',
        specimen: 'Helper text, timestamps, form field hints',
        when: 'Secondary information: help text beneath form inputs, timestamps, captions. Never use for primary readable content.',
      },
      {
        key: 'tiny',
        token: 'theme.fonts.size.tiny',
        specimen: 'Badge labels, count indicators',
        when: 'De-emphasized metadata: version badges, counts in compact lists, table sub-labels. Use sparingly.',
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
        specimen: 'Navigation item',
        when: 'Link and dropdown labels in the navigation. Applied by navigation components — do not set manually.',
      },
    ],
  },
];

const SIZE_KEY_LABELS: Record<FontSizeKey, string> = {
  h1: 'Heading 1',
  h2: 'Heading 2',
  h3: 'Heading 3',
  h4: 'Heading 4',
  h5: 'Heading 5',
  h6: 'Heading 6',
  huge: 'Huge',
  large: 'Large',
  body: 'Body',
  small: 'Small',
  tiny: 'Tiny',
  navigation: 'Navigation',
};

const TypeScaleDoc = () => {
  const theme = useTheme();

  return (
    <div>
      {TYPE_SCALE_GROUPS.map((group) => (
        <ScaleGroup key={group.label}>
          <GroupHeading>{group.label}</GroupHeading>
          <GroupNote>{group.note}</GroupNote>
          {group.items.map(({ key, token, family, specimen, when }) => {
            const sizes = theme.fonts.size as Record<string, string>;
            const families = theme.fonts.family as Record<string, string>;
            const sizeValue = sizes[key] ?? '1rem';
            const familyValue = family ? families[family] : families.body;

            return (
              <ScaleItem key={key}>
                <ScaleMeta>
                  <Token>{token}</Token>
                  <PxLabel>
                    {sizeValue} / {remToPx(sizeValue)}
                  </PxLabel>
                </ScaleMeta>
                <p
                  style={{
                    fontFamily: familyValue,
                    fontSize: sizeValue,
                    color: theme.colors.text.primary,
                    margin: '0 0 4px',
                    lineHeight: 1.3,
                  }}>
                  {SIZE_KEY_LABELS[key]}: {specimen}
                </p>
                <UsageNote>
                  <Strong>Use when: </Strong>
                  {when}
                </UsageNote>
              </ScaleItem>
            );
          })}
        </ScaleGroup>
      ))}
    </div>
  );
};

// ─── Text Colors ───────────────────────────────────────────────────────────

const ColorItem = styled.div`
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 16px;
  padding: 20px 0;
  align-items: start;

  & + & {
    border-top: 1px solid rgba(128, 128, 128, 0.15);
  }
`;

const Swatch = styled.div<{ $color: string }>`
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background-color: ${({ $color }) => $color};
  border: 1px solid rgba(128, 128, 128, 0.25);
  flex-shrink: 0;
`;

type ColorEntry = {
  token: string;
  getColor: (theme: any) => string;
  specimen: string;
  when: string;
  not: string;
};

const TEXT_COLORS: ColorEntry[] = [
  {
    token: 'theme.colors.text.primary',
    getColor: (t) => t.colors.text.primary,
    specimen: 'Primary text carries the essential information the user must read.',
    when: 'All primary readable content: headings, body copy, button labels, input values, table cell data. This is the default — use it unless the text is explicitly supporting or de-emphasized.',
    not: 'Labels and hints that support a primary value — those should use secondary.',
  },
  {
    token: 'theme.colors.text.secondary',
    getColor: (t) => t.colors.text.secondary,
    specimen: 'Secondary text supports and contextualizes the primary content.',
    when: 'Supporting information: form field hints, timestamps below a title, column sub-labels, empty-state descriptions. Signals "this is contextual, not essential".',
    not: 'Content the user needs to act on. If they must read it to complete a task, use primary.',
  },
  {
    token: 'theme.colors.text.disabled',
    getColor: (t) => t.colors.text.disabled,
    specimen: 'Disabled text indicates an unavailable state.',
    when: 'Text inside disabled form inputs, greyed-out menu items, or any element the user cannot currently interact with.',
    not: 'De-emphasized but readable content — use secondary instead. Disabled implies "not interactive", not "less important".',
  },
];

const TextColorsDoc = () => {
  const theme = useTheme();

  return (
    <div>
      {TEXT_COLORS.map(({ token, getColor, specimen, when, not }) => {
        const color = getColor(theme);

        return (
          <ColorItem key={token}>
            <Swatch $color={color} />
            <div>
              <Token>{token}</Token>
              <p
                style={{
                  fontFamily: theme.fonts.family.body,
                  fontSize: theme.fonts.size.body,
                  color,
                  margin: '8px 0 4px',
                  lineHeight: 1.4,
                }}>
                {specimen}
              </p>
              <UsageNote>
                <Strong>Use when: </Strong>
                {when}
              </UsageNote>
              <UsageNote>
                <Strong>Not for: </Strong>
                {not}
              </UsageNote>
            </div>
          </ColorItem>
        );
      })}
    </div>
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
