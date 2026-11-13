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

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';

import { FoundationTable } from '../../Foundation/shared';
import { dividerColor } from '../../Foundation/lumaTokens';

// ─── Example button ────────────────────────────────────────────────────────
// Thin wrapper around the product `Button` from `components/bootstrap`, so every example on this page
// renders the real component. It maps the doc's hierarchy/intent names to the component's `bsStyle`.

type Variant = 'primary' | 'secondary' | 'tertiary' | 'neutral' | 'destructive' | 'destructiveSecondary';
type Size = 'xs' | 'sm' | 'md';

const SIZES: Record<Size, { icon: number }> = {
  xs: { icon: 14 },
  sm: { icon: 16 },
  md: { icon: 20 },
};

const BS_STYLE: Record<Variant, React.ComponentProps<typeof Button>['bsStyle']> = {
  primary: 'primary',
  secondary: 'default',
  neutral: 'default',
  tertiary: 'transparent',
  destructive: 'danger',
  // No dedicated destructive-secondary style exists in the component yet.
  destructiveSecondary: 'danger',
};

type ExampleButtonProps = {
  children?: React.ReactNode;
  variant?: Variant;
  size?: Size;
  icon?: string;
  trailingIcon?: string;
  loading?: boolean;
  disabled?: boolean;
  'aria-label'?: string;
};

export const ExampleButton = ({
  children = undefined,
  variant = 'primary',
  size = 'xs',
  icon = undefined,
  trailingIcon = undefined,
  loading = false,
  disabled = false,
  'aria-label': ariaLabel = undefined,
}: ExampleButtonProps) => {
  const iconSize = SIZES[size].icon;
  const leading = loading ? 'progress_activity' : icon;
  const gap = children ? 4 : 0;

  return (
    <Button type="button" bsStyle={BS_STYLE[variant]} bsSize={size} disabled={disabled} aria-label={ariaLabel}>
      {leading && <Icon name={leading as any} spin={loading} style={{ fontSize: iconSize, marginRight: gap }} />}
      {children}
      {trailingIcon && <Icon name={trailingIcon as any} style={{ fontSize: iconSize, marginLeft: gap }} />}
    </Button>
  );
};

const LumaLink = styled.a`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xxs};
  font-size: 14px;
  color: ${({ theme }) => theme.colors.global.link};
  text-decoration: underline;
`;

// ─── Layout primitives ─────────────────────────────────────────────────────

const Grid = styled.div<{ $cols: number }>`
  display: grid;
  grid-template-columns: repeat(${({ $cols }) => $cols}, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacings.md};
  margin: ${({ theme }) => `${theme.spacings.md} 0`};

  @media (max-width: 800px) {
    grid-template-columns: 1fr;
  }
`;

const CardBox = styled.div`
  display: flex;
  flex-direction: column;
  padding: ${({ theme }) => theme.spacings.sm};
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  border-radius: 8px;
`;

const Preview = styled.div<{ $align?: 'start' | 'end' | 'center' }>`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: ${({ $align = 'start' }) => ({ start: 'flex-start', end: 'flex-end', center: 'center' })[$align]};
  gap: ${({ theme }) => theme.spacings.sm};
  min-height: 88px;
  padding: ${({ theme }) => theme.spacings.md};
  margin-bottom: ${({ theme }) => theme.spacings.sm};
  border-radius: 6px;
  background: rgba(128, 128, 128, 0.08);
`;

const TONE_COLORS = { do: '#1A9869', dont: '#E74342' };

const CardTitle = styled.div<{ $tone?: 'do' | 'dont' }>`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
  margin-bottom: ${({ theme }) => theme.spacings.xs};
  font-weight: 700;
  color: ${({ $tone }) => ($tone ? TONE_COLORS[$tone] : 'inherit')};
`;

const CardBody = styled.div`
  flex: 1;
  line-height: 1.5;

  p {
    margin: 0 0 ${({ theme }) => theme.spacings.xs};
  }
`;

const CardFooter = styled.div`
  margin-top: ${({ theme }) => theme.spacings.sm};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

type CardProps = {
  preview?: React.ReactNode;
  previewAlign?: 'start' | 'end' | 'center';
  title: React.ReactNode;
  tone?: 'do' | 'dont';
  footer?: React.ReactNode;
  children: React.ReactNode;
};

const Card = ({
  preview = undefined,
  previewAlign = undefined,
  title,
  tone = undefined,
  footer = undefined,
  children,
}: CardProps) => (
  <CardBox>
    {preview && <Preview $align={previewAlign}>{preview}</Preview>}
    <CardTitle $tone={tone}>
      {tone === 'do' && <Icon name="check" />}
      {tone === 'dont' && <Icon name="close" />}
      {title}
    </CardTitle>
    <CardBody>{children}</CardBody>
    {footer && <CardFooter>{footer}</CardFooter>}
  </CardBox>
);

const Code = styled.code`
  font-size: 12px;
  padding: 1px ${({ theme }) => theme.spacings.xs};
  border-radius: 3px;
  background: rgba(128, 128, 128, 0.15);
`;

const Row = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.spacings.xs};
  align-items: center;
`;

// ─── Hierarchy ─────────────────────────────────────────────────────────────

export const Hierarchy = () => (
  <Grid $cols={3}>
    <Card
      preview={<ExampleButton icon="add">Create stream</ExampleButton>}
      title="Primary"
      footer="Examples: Save changes, Create stream, Run search">
      The most important action on a surface. Use at most one per view, modal, or distinct section.
    </Card>
    <Card
      preview={
        <ExampleButton variant="neutral" icon="download">
          Export
        </ExampleButton>
      }
      title="Secondary"
      footer="Examples: Cancel, Export, Add filter">
      Supporting actions that matter but shouldn&apos;t compete with the primary, such as &quot;Cancel&quot; next to
      &quot;Save,&quot; or &quot;Export&quot; in a toolbar.
    </Card>
    <Card
      preview={<ExampleButton variant="tertiary">Reset filters</ExampleButton>}
      title="Tertiary"
      footer="Examples: Reset filters, Show details, Copy ID">
      Low-emphasis actions, often in dense layouts or alongside other buttons, where a filled style would add visual
      noise.
    </Card>
  </Grid>
);

// ─── Intent ────────────────────────────────────────────────────────────────

export const Intent = () => (
  <Grid $cols={2}>
    <Card
      preview={
        <ExampleButton variant="destructive" icon="delete">
          Delete stream
        </ExampleButton>
      }
      title="Destructive"
      footer="Examples: Delete stream, Remove user, Purge index">
      For actions that delete data or can&apos;t easily be undone. Always follow with a confirmation dialog or offer
      undo, and name the consequence in the label.
    </Card>
    <Card
      preview={
        <ExampleButton variant="destructiveSecondary" icon="delete">
          Remove
        </ExampleButton>
      }
      title="Destructive Secondary"
      footer="Examples: Remove, Detach rule, Clear selection">
      A quieter destructive style for repeated or lower-risk actions, such as removing an item from a list, where a
      solid red button on every row would overwhelm the page.
    </Card>
  </Grid>
);

// ─── Button or link ────────────────────────────────────────────────────────

export const ButtonOrLink = () => (
  <Grid $cols={2}>
    <Card
      preview={
        <>
          <ExampleButton variant="neutral">Add filter</ExampleButton>
          <ExampleButton>Save changes</ExampleButton>
        </>
      }
      title="Use a Button"
      footer={
        <>
          Rendered as <Code>{'<Button>'}</Code>
        </>
      }>
      When the action changes something: submits data, opens a modal or menu, toggles a setting, or deletes.
    </Card>
    <Card
      preview={
        <LumaLink href="#" onClick={(e) => e.preventDefault()}>
          Read the pipeline docs <Icon name="open_in_new" style={{ fontSize: 14 }} />
        </LumaLink>
      }
      title="Use a Link"
      footer={
        <>
          Rendered as <Code>{'<a href>'}</Code>
        </>
      }>
      If the destination is known before the click and nothing else happens, use a link. If the click does something and
      navigation follows, use a button.
    </Card>
  </Grid>
);

type LinkExample = { example: string; use: 'Link' | 'Button'; why: string };

const LINK_EXAMPLES: LinkExample[] = [
  { example: 'Streams (main nav)', use: 'Link', why: 'Known destination' },
  { example: 'View stream details', use: 'Link', why: 'Known destination' },
  { example: 'Read the docs', use: 'Link', why: 'External destination' },
  { example: 'Save changes', use: 'Button', why: 'Action, then redirect' },
  { example: 'Open settings modal', use: 'Button', why: 'Stays on the page' },
  { example: 'System (nav dropdown)', use: 'Button', why: 'Opens a menu' },
  { example: 'Export results', use: 'Button', why: 'Generates a file on click' },
  { example: 'Download content pack', use: 'Link', why: 'File already exists' },
];

export const LinkExamples = () => (
  <FoundationTable
    columns={[
      { header: 'Example', render: (r: LinkExample) => r.example },
      { header: 'Use', width: '180px', render: (r: LinkExample) => r.use },
      { header: 'Why', render: (r: LinkExample) => r.why },
    ]}
    rows={LINK_EXAMPLES}
    keyBy={(r) => r.example}
  />
);

// ─── Sizes ─────────────────────────────────────────────────────────────────

const SizeDemoBox = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  align-items: end;
  padding: ${({ theme }) => theme.spacings.md};
  margin: ${({ theme }) => `${theme.spacings.md} 0`};
  border: 1px dashed ${({ theme }) => dividerColor(theme)};
  border-radius: 8px;
  text-align: center;
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: 12px;
`;

const SizeColumn = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

export const SizesDemo = () => (
  <SizeDemoBox>
    {(
      [
        ['xs', 'Extra small'],
        ['sm', 'Small'],
        ['md', 'Medium'],
      ] as const
    ).map(([size, label]) => (
      <SizeColumn key={size}>
        {size === 'sm' && <span>Default</span>}
        <ExampleButton size={size} icon="add">
          {label}
        </ExampleButton>
        <span>size=&quot;{size}&quot;</span>
        <span>{`<Icon size={${SIZES[size].icon}} />`}</span>
      </SizeColumn>
    ))}
  </SizeDemoBox>
);

type SizeRow = { size: Size; height: string; font: string; icon: string; usage: string };

const SIZE_ROWS: SizeRow[] = [
  { size: 'xs', height: '24', font: '13.17', icon: '14', usage: 'Tables, cards, infographics, dense toolbars' },
  { size: 'sm', height: '29.41', font: '14.05', icon: '16', usage: 'Forms, modals, page headers, most interfaces' },
  { size: 'md', height: '33.83', font: '16', icon: '20', usage: 'Empty states, prominent standalone actions' },
];

export const SizesTable = () => (
  <FoundationTable
    columns={[
      { header: 'Size', width: '100px', render: (r: SizeRow) => <Code>{r.size}</Code> },
      { header: 'Height', width: '100px', render: (r: SizeRow) => r.height },
      { header: 'Font Size', width: '100px', render: (r: SizeRow) => r.font },
      { header: 'Icon', width: '80px', render: (r: SizeRow) => r.icon },
      { header: 'Typically Used For', render: (r: SizeRow) => r.usage },
    ]}
    rows={SIZE_ROWS}
    keyBy={(r) => r.size}
  />
);

// ─── States ────────────────────────────────────────────────────────────────

type StateRow = { label: string; variant: Variant };

const STATE_ROWS: StateRow[] = [
  { label: 'Primary', variant: 'primary' },
  { label: 'Secondary', variant: 'secondary' },
  { label: 'Tertiary', variant: 'tertiary' },
  { label: 'Destructive', variant: 'destructive' },
];

export const StatesTable = () => (
  <FoundationTable
    columns={[
      { header: '', width: '160px', render: (r: StateRow) => r.label },
      {
        header: 'Default',
        width: '120px',
        render: (r: StateRow) => <ExampleButton variant={r.variant}>Button</ExampleButton>,
      },
      {
        header: 'Disabled',
        width: '120px',
        render: (r: StateRow) => (
          <ExampleButton variant={r.variant} disabled>
            Button
          </ExampleButton>
        ),
      },
      {
        header: 'Loader',
        render: (r: StateRow) => (
          <ExampleButton variant={r.variant} loading>
            Button
          </ExampleButton>
        ),
      },
    ]}
    rows={STATE_ROWS}
    keyBy={(r) => r.label}
  />
);

export const StateRules = () => (
  <Grid $cols={3}>
    <Card title="Hover and Pressed">
      Each step darkens the fill while keeping label contrast at 4.5:1 or higher. Never rely on hover alone to show that
      something is clickable.
    </Card>
    <Card title="Disabled">
      Use sparingly. Prefer <Code>aria-disabled=&quot;true&quot;</Code> with a tooltip explaining why, so the button
      stays focusable and screen reader users can find out what&apos;s blocking them.
    </Card>
    <Card title="Loading">
      Replace the leading icon with a spinner and keep the label and width fixed. Set{' '}
      <Code>aria-busy=&quot;true&quot;</Code> and ignore repeat clicks until the request resolves.
    </Card>
  </Grid>
);

// ─── Icons ─────────────────────────────────────────────────────────────────

export const Icons = () => (
  <Grid $cols={2}>
    <Card
      preview={
        <>
          <ExampleButton variant="secondary" icon="download">
            Export
          </ExampleButton>
          <ExampleButton icon="add">Create alert</ExampleButton>
        </>
      }
      title="Leading Icon">
      Reinforces the action. Pick a familiar icon that matches the verb, and leave it out if no icon clearly fits.
    </Card>
    <Card
      preview={
        <>
          <ExampleButton variant="tertiary" trailingIcon="arrow_drop_down">
            Actions
          </ExampleButton>
          <ExampleButton variant="secondary" trailingIcon="arrow_drop_down">
            Actions
          </ExampleButton>
        </>
      }
      title="Trailing Icon">
      Signals what happens next: a dropdown menu, a disclosure, or a direction. Don&apos;t use a trailing icon as
      decoration.
    </Card>
    <Card preview={<ExampleButton icon="add" aria-label="Add" />} title="Icon-Only">
      See Action Icon
    </Card>
    <Card
      preview={
        <ExampleButton icon="check" trailingIcon="arrow_forward">
          Button
        </ExampleButton>
      }
      title="Don’t"
      tone="dont">
      Avoid icons on both sides unless the trailing icon opens a menu. Two decorative icons add noise without adding
      meaning.
    </Card>
  </Grid>
);

// ─── Labels ────────────────────────────────────────────────────────────────

type LabelRow = { guideline: string; good: React.ReactNode; bad: React.ReactNode };

const LABEL_ROWS: LabelRow[] = [
  {
    guideline: 'Start with a verb',
    good: <ExampleButton variant="secondary">Create stream</ExampleButton>,
    bad: <ExampleButton variant="secondary">New stream</ExampleButton>,
  },
  {
    guideline: 'Be specific about the outcome',
    good: <ExampleButton>Save changes</ExampleButton>,
    bad: <ExampleButton>OK</ExampleButton>,
  },
  {
    guideline: 'Use sentence case',
    good: <ExampleButton variant="secondary">Add source</ExampleButton>,
    bad: <ExampleButton variant="secondary">Add Source</ExampleButton>,
  },
  {
    guideline: 'Three words max',
    good: <ExampleButton variant="secondary">Export CSV</ExampleButton>,
    bad: <ExampleButton variant="secondary">Click here to export as CSV</ExampleButton>,
  },
  {
    guideline: 'Repeat the action in confirmations',
    good: (
      <Row>
        <ExampleButton variant="neutral">Cancel</ExampleButton>
        <ExampleButton variant="destructive">Delete index set</ExampleButton>
      </Row>
    ),
    bad: (
      <Row>
        <ExampleButton variant="neutral">No</ExampleButton>
        <ExampleButton variant="destructive">Yes</ExampleButton>
      </Row>
    ),
  },
];

export const LabelsTable = () => (
  <FoundationTable
    columns={[
      { header: 'Guideline', width: '30%', render: (r: LabelRow) => r.guideline },
      {
        header: '✓ Do',
        width: '30%',
        render: (r: LabelRow) => r.good,
      },
      { header: '✕ Don’t', render: (r: LabelRow) => r.bad },
    ]}
    rows={LABEL_ROWS}
    keyBy={(r) => r.guideline}
  />
);

// ─── Accessibility ─────────────────────────────────────────────────────────

export const Accessibility = () => (
  <Grid $cols={2}>
    <Card title="Semantics and Keyboard">
      <p>
        Use the native <Code>{'<button>'}</Code> with <Code>type=&quot;button&quot;</Code> unless it submits a form.
      </p>
      <p>Enter and Space activate the button. Tab order follows the visual order.</p>
      <p>
        Icon-only buttons need an <Code>aria-label</Code>. Decorative icons get{' '}
        <Code>aria-hidden=&quot;true&quot;</Code>.
      </p>
      <p>
        While loading, set <Code>aria-busy=&quot;true&quot;</Code> and announce the result in a live region.
      </p>
    </Card>
    <Card title="Visual">
      <p>Label text reaches 4.5:1 against its fill in every state except disabled.</p>
      <p>Targets are at least 24×24px; 32px or larger is preferred.</p>
      <p>Color never carries meaning alone: destructive labels name the consequence.</p>
    </Card>
  </Grid>
);

// ─── Placement ─────────────────────────────────────────────────────────────

const MockSurface = styled.div<{ $shadow?: boolean }>`
  width: 100%;
  padding: ${({ theme }) => theme.spacings.sm};
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  border-radius: 6px;
  background: ${({ theme }) => theme.colors.global.contentBackground};
  ${({ $shadow }) => $shadow && 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);'}
`;

const MockTitle = styled.div<{ $large?: boolean }>`
  font-size: ${({ $large }) => ($large ? '20px' : '16px')};
  margin-bottom: ${({ theme }) => theme.spacings.xs};
`;

const MockInput = styled.div`
  padding: ${({ theme }) => `${theme.spacings.xs} ${theme.spacings.sm}`};
  margin: ${({ theme }) => `${theme.spacings.xxs} 0 ${theme.spacings.sm}`};
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  border-radius: 4px;
`;

const SkeletonLine = styled.div<{ $width: string }>`
  height: 8px;
  width: ${({ $width }) => $width};
  margin-top: ${({ theme }) => theme.spacings.xs};
  border-radius: 4px;
  background: rgba(128, 128, 128, 0.25);
`;

const MockRow = styled.div<{ $head?: boolean }>`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: ${({ theme }) => theme.spacings.xs} 0;
  border-bottom: 1px solid ${({ theme }) => dividerColor(theme)};
  color: ${({ $head, theme }) => ($head ? theme.colors.text.secondary : 'inherit')};

  &:last-child {
    border-bottom: 0;
  }
`;

const Spread = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const Right = styled(Row)`
  justify-content: flex-end;
`;

export const Placement = () => (
  <Grid $cols={2}>
    <Card
      preview={
        <MockSurface $shadow>
          <MockTitle $large>Delete index set?</MockTitle>
          <p>This permanently removes the index set and every message stored in it.</p>
          <Right>
            <ExampleButton variant="neutral" size="sm">
              Cancel
            </ExampleButton>
            <ExampleButton variant="destructive" size="sm">
              Delete index set
            </ExampleButton>
          </Right>
        </MockSurface>
      }
      title="Modals and Dialogs">
      Right-align actions in the footer. The primary or destructive action goes on the far right, with Cancel to its
      left.
    </Card>
    <Card
      preview={
        <MockSurface>
          <strong>Stream title</strong>
          <MockInput>Firewall events</MockInput>
          <Row>
            <ExampleButton variant="neutral" size="sm">
              Cancel
            </ExampleButton>
            <ExampleButton size="sm">Create stream</ExampleButton>
          </Row>
        </MockSurface>
      }
      title="Full-Page Forms">
      Left-align actions under the fields so they sit on the user&apos;s reading path, with the secondary first.
    </Card>
    <Card
      preview={
        <MockSurface>
          <Spread>
            <MockTitle $large>Streams</MockTitle>
            <Row>
              <ExampleButton variant="tertiary" size="sm" icon="download">
                Export
              </ExampleButton>
              <ExampleButton size="sm" icon="add">
                Create stream
              </ExampleButton>
            </Row>
          </Spread>
          <SkeletonLine $width="75%" />
          <SkeletonLine $width="45%" />
        </MockSurface>
      }
      title="Page Headers and Toolbars">
      Right-align page-level actions in the header, with the primary on the far right. One primary per page.
    </Card>
    <Card
      preview={
        <MockSurface>
          <MockRow $head>
            <span>Stream</span>
            <span>Actions</span>
          </MockRow>
          {['Firewall events', 'Authentication logs'].map((name) => (
            <MockRow key={name}>
              <span>{name}</span>
              <Row>
                <ExampleButton variant="tertiary">Edit</ExampleButton>
                <ExampleButton variant="tertiary" icon="more_horiz" aria-label="More actions" />
              </Row>
            </MockRow>
          ))}
        </MockSurface>
      }
      title="Table Rows">
      Use xs tertiary buttons. Move destructive and rarely used actions into an overflow menu so they aren&apos;t
      clicked by accident.
    </Card>
    <Card
      previewAlign="end"
      preview={
        <>
          <ExampleButton variant="neutral">Save draft</ExampleButton>
          <ExampleButton>Publish</ExampleButton>
        </>
      }
      title="Do"
      tone="do">
      Pair one primary with secondary or tertiary actions, so the most important choice is obvious.
    </Card>
    <Card
      previewAlign="end"
      preview={
        <>
          <ExampleButton>Save draft</ExampleButton>
          <ExampleButton>Publish</ExampleButton>
        </>
      }
      title="Don’t"
      tone="dont">
      Don&apos;t place two primary buttons side by side. When everything is emphasized, nothing is.
    </Card>
  </Grid>
);
