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
import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { Canvas } from '@storybook/addon-docs/blocks';
import { MemoryRouter } from 'react-router-dom';
import styled from 'styled-components';

import { Alert, Button, Input } from 'components/bootstrap';
import { CreateModal, CreatePage } from 'components/common';

import Mermaid from './Mermaid';

// ─── Pattern Doc UI ───────────────────────────────────────────────────────────

const DocContainer = styled.div`
  max-width: 900px;
  padding: 1rem;
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

  ${ContextCard} + ${ContextCard} {
    margin-top: ${({ theme }) => theme.spacings.lg};
  }
`;

const ContextBody = styled.p`
  line-height: 1.6;
  margin: 0 0 ${({ theme }) => theme.spacings.md};
`;

const CharacteristicHeading = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: 0 0 ${({ theme }) => theme.spacings.xs};
`;

const CharacteristicList = styled.ul`
  list-style-type: disc;
  margin: 0 0 ${({ theme }) => theme.spacings.md};
  padding-left: ${({ theme }) => theme.spacings.lg};

  li {
    line-height: 1.5;
    margin-bottom: ${({ theme }) => theme.spacings.xs};
  }
`;

// ─── Flowchart ────────────────────────────────────────────────────────────────

const DIAGRAM = `flowchart TD
  A(["User's current view"])
  A --> B{{"\`Should users remain
in their current workflow?\`"}}
  B -->|Yes| MODAL
  B -->|No| PAGE

  subgraph Surface["Surface"]
    MODAL(["\`**Modal**
Current context preserved\`"])
    PAGE(["\`**Page**
Dedicated creation experience\`"])
  end

  subgraph Method["Method"]
    MW(["\`**Form or Wizard**
Single-step or guided workflow\`"])
    FW(["\`**Form or Wizard**
Single-step or guided workflow\`"])
  end

  MODAL --> MW
  PAGE --> FW

  MW --> RC(["\`**Return to current workflow**
Modal closes, toast shown\`"])
  FW --> NC(["\`**Navigate to entity**
Details page, toast shown\`"])
`;

// ─── Form fields used in the interactive examples ─────────────────────────────

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

// ─── Stories (hidden from sidebar, embedded via Canvas in the docs page) ──────

export const CurrentContextModal: StoryObj = {
  tags: ['!dev'],
  render: () => {
    const [show, setShow] = useState(false);

    return (
      <>
        <Button onClick={() => setShow(true)}>Create Stream</Button>
        <CreateModal<StoryValues>
          entityName="Stream"
          show={show}
          onClose={() => setShow(false)}
          initialValues={{ title: '', description: '' }}
          onSubmit={async () => {}}>
          <FormFields />
        </CreateModal>
      </>
    );
  },
};

export const NewContextPage: StoryObj = {
  tags: ['!dev'],
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
      detailsRoute={(id) => `/streams/${id}`}
      initialValues={{ title: '', description: '' }}
      onSubmit={async () => ({ id: 'new-stream-id' })}
      description="Streams route incoming messages into categories. Route a message into a stream by applying matching rules.">
      <FormFields />
    </CreatePage>
  ),
};

// ─── Docs page ────────────────────────────────────────────────────────────────

const CreateEntityPattern = () => (
  <DocContainer>
    <DocH1>Creating an Entity</DocH1>

    <DocLead>
      Entity creation is a foundational interaction in Graylog. This guide helps you decide how to implement a creation
      flow consistently.
    </DocLead>

    <Alert bsStyle="info">
      <strong>Design principle:</strong> Entity creation is organized around two decisions: choose the appropriate
      surface, then choose the appropriate method.
    </Alert>

    <DiagramWrapper>
      <Mermaid chart={DIAGRAM} />
    </DiagramWrapper>

    <ContextGrid>
      <SectionGroup>
        <DocH2>Choosing the Surface</DocH2>

        <ContextBody>
          Choose the surface based on whether creation should happen within the user's current workflow or in a
          dedicated creation experience.
        </ContextBody>

        <ContextCard>
          <DocH3 id="current-context">Current Context: Modal</DocH3>

          <ContextBody>
            Use a Modal when creation is part of the task the user is already performing and they should be able to
            continue where they left off when creation is complete.
          </ContextBody>

          <CharacteristicHeading>Characteristics</CharacteristicHeading>
          <CharacteristicList>
            <li>Users remain in their current workflow.</li>
            <li>The parent view remains visible.</li>
            <li>Creation is completed without changing pages.</li>
            <li>Completion returns users to their previous location.</li>
            <li>Successful creation closes the modal and shows a toast notification.</li>
          </CharacteristicList>

          <Canvas of={CurrentContextModal} />
        </ContextCard>

        <ContextCard>
          <DocH3 id="new-context">New Context: Page</DocH3>

          <ContextBody>
            Use a Page when creation benefits from a dedicated experience with its own focus, navigation state, and
            destination.
          </ContextBody>

          <CharacteristicHeading>Characteristics</CharacteristicHeading>
          <CharacteristicList>
            <li>Users move into a focused creation experience.</li>
            <li>The flow has its own URL and browser history entry.</li>
            <li>Additional guidance, content, or validation can be accommodated.</li>
            <li>Successful creation navigates users to the newly created entity.</li>
            <li>A toast notification communicates successful creation.</li>
          </CharacteristicList>

          <Canvas of={NewContextPage} />
        </ContextCard>

        <Alert bsStyle="info">
          If it is unclear whether creation belongs in the current context or a dedicated one, involve Product and
          Design before implementation. Context decisions affect navigation, workflow continuity, and long-term
          consistency across the product.
        </Alert>
      </SectionGroup>

      <SectionGroup>
        <DocH2>Choosing the Method</DocH2>

        <ContextBody>
          After selecting the surface, choose the creation method. A Form and a Wizard can both be used in either a
          Modal or a Page.
        </ContextBody>

        <ContextCard>
          <DocH3>Form</DocH3>

          <ContextBody>Use a Form when users can reasonably complete creation in a single step.</ContextBody>

          <CharacteristicHeading>Characteristics</CharacteristicHeading>
          <CharacteristicList>
            <li>Fields can be displayed together.</li>
            <li>There are no significant dependencies between steps.</li>
            <li>The workflow is primarily data entry.</li>
          </CharacteristicList>
        </ContextCard>

        <ContextCard>
          <DocH3>Wizard</DocH3>

          <ContextBody>Use a Wizard when users benefit from progressive guidance and decision making.</ContextBody>

          <CharacteristicHeading>Characteristics</CharacteristicHeading>
          <CharacteristicList>
            <li>The flow contains multiple dependent decisions.</li>
            <li>Information is naturally grouped into stages.</li>
            <li>Showing everything at once would be overwhelming.</li>
            <li>Users benefit from being guided through the process.</li>
          </CharacteristicList>
        </ContextCard>

        <ContextCard>
          <DocH3>Avoid Wizards When</DocH3>

          <CharacteristicList>
            <li>The only purpose is reducing scroll length.</li>
            <li>Steps do not depend on one another.</li>
            <li>Users would frequently need to jump between sections.</li>
            <li>The workflow can be understood in a single view.</li>
          </CharacteristicList>
        </ContextCard>
      </SectionGroup>
    </ContextGrid>
  </DocContainer>
);

// ─── Meta ─────────────────────────────────────────────────────────────────────

const meta: Meta = {
  title: 'Patterns/Creating an Entity',
  tags: ['autodocs'],
  parameters: {
    layout: 'padded',
    docs: {
      page: CreateEntityPattern,
    },
  },
};

export default meta;
