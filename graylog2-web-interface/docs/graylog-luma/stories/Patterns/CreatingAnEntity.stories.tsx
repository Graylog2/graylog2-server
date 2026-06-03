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
import { fn } from 'storybook/test';
import { MemoryRouter } from 'react-router-dom';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';
import { CreatePage } from 'components/common';

import Mermaid from './Mermaid';

// ─── Pattern Doc UI ───────────────────────────────────────────────────────────

const DocContainer = styled.div`
  max-width: 900px;
`;

const DocH1 = styled.h1`
  margin: 0 0 ${({ theme }) => theme.spacings.xs};
`;

const DocH2 = styled.h2`
  margin: 0 0 ${({ theme }) => theme.spacings.sm};
`;

const DocH3 = styled.h3`
  margin: 0 0 ${({ theme }) => theme.spacings.sm};
`;

const DocLead = styled.p`
  line-height: 1.6;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.xl};
`;

const DiagramWrapper = styled.div`
  margin-bottom: ${({ theme }) => theme.spacings.xl};
`;

const ContextGrid = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.lg};
`;

const ContextCard = styled.div``;

const SectionGroup = styled.div`
  display: flex;
  flex-direction: column;
`;

const ContextLabel = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.xs};
`;

const ContextBody = styled.p`
  line-height: 1.6;
  margin: 0 0 ${({ theme }) => theme.spacings.md};
`;

const RuleListHeading = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.xs};
`;

const RuleList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0 0 ${({ theme }) => theme.spacings.md};
`;

const RuleItem = styled.li<{ $type: 'do' | 'dont' }>`
  padding: ${({ theme }) => theme.spacings.xs} 0;
  padding-left: 1.4em;
  position: relative;
  line-height: 1.5;

  &::before {
    position: absolute;
    left: 0;
    font-weight: 700;
    ${({ $type }) => ($type === 'do' ? "content: '✓'; color: #22c55e;" : "content: '✕'; color: #ef4444;")}
  }
`;

// ─── Flowchart ────────────────────────────────────────────────────────────────

const DIAGRAM = `flowchart TD
  A(["User's current view"])
  A --> B{{"\`Must keep user
in current context?\`"}}
  B -->|Yes| MODAL(["\`**Modal**
Current context preserved\`"])
  B -->|No| PAGE(["\`**Page**
New context, full focus\`"])
  MODAL --> MW(["\`**Form or Wizard**
No secondary context or workflow\`"])
  PAGE --> FORM(["\`**Form**
Static or dynamic fields\`"])
  PAGE --> WIZARD(["\`**Wizard**
Multi-step, guided flow\`"])
  MW --> RC(["\`**Return to current context**
Modal closes, toast shown\`"])
  FORM & WIZARD --> NC(["\`**Navigate to new context**
Detail page, list, or origin\`"])
`;

// ─── Pattern Overview Doc ────────────────────────────────────────────────────

const CreateEntityPatternDoc = () => (
  <DocContainer>
    <DocH1>Creating an Entity</DocH1>
    <DiagramWrapper>
      <Mermaid chart={DIAGRAM} />
    </DiagramWrapper>

    <ContextGrid>
      <SectionGroup>
        <DocH2>Surface</DocH2>

        <ContextCard>
          <DocH3 id="current-context">Modal</DocH3>
          <ContextBody>
            Use a Modal to keep the user in their current context. The parent view stays visible, and on completion the
            modal closes and returns the user to where they started. Use a Form or Wizard — if the workflow requires its
            own context, use a Page instead.
          </ContextBody>

          <RuleListHeading>Do</RuleListHeading>
          <RuleList>
            <RuleItem $type="do">Show a toast notification on successful creation</RuleItem>
          </RuleList>

          <RuleListHeading>Do not</RuleListHeading>
          <RuleList>
            <RuleItem $type="dont">Open a modal on top of another modal</RuleItem>
          </RuleList>
        </ContextCard>

        <ContextCard>
          <DocH3 id="new-context">Page</DocH3>
          <ContextBody>
            Use a Page when a context shift is appropriate. The user gets a dedicated creation experience and on
            completion is taken to the entity detail page. Pages have a browser history entry and can be directly linked
            to.
          </ContextBody>
        </ContextCard>
      </SectionGroup>

      <SectionGroup>
        <DocH2>Method</DocH2>

        <ContextCard>
          <DocH3>Form vs Wizard</DocH3>
          <ContextBody>
            Use a Form for single-step creation with a manageable number of fields. Use a Wizard when creation involves
            multiple dependent steps or too many fields to show at once.
          </ContextBody>
        </ContextCard>
      </SectionGroup>
    </ContextGrid>
  </DocContainer>
);

// ─── Form fields used in the interactive example ─────────────────────────────

type StoryValues = { title: string; description: string };

const FormFields = () => (
  <>
    <Input
      type="text"
      id="title"
      name="title"
      label="Title"
      placeholder="e.g. Production errors"
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9"
      required
    />
    <Input
      type="text"
      id="description"
      name="description"
      label="Description"
      placeholder="What does this stream collect?"
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9"
    />
  </>
);

// ─── Meta & Stories ───────────────────────────────────────────────────────────

const meta: Meta = {
  title: 'Patterns/Creating an Entity',
  parameters: {
    layout: 'padded',
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Overview: Story = {
  render: () => <CreateEntityPatternDoc />,
};

export const NewContextPage: Story = {
  name: 'New Context — Page',
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
  render: () => (
    <CreatePage<StoryValues>
      entityName="Stream"
      overviewRoute="/streams"
      initialValues={{ title: '', description: '' }}
      onSubmit={fn()}
      description="Streams route incoming messages into categories. Route a message into a stream by applying matching rules.">
      <FormFields />
    </CreatePage>
  ),
};
