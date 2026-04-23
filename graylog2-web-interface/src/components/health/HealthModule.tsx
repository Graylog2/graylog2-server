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
import { Tree, useTree } from '@mantine/core';
import styled, { css } from 'styled-components';

import { Col, Row } from 'components/bootstrap';
import { Icon } from 'components/common';

import HealthStatusIcon from './HealthStatusIcon';
import {
  mockHealthTree,
  mockHealthTreeData,
  mockHealthTreeInitialExpandedState,
  mockHealthTreeLookup,
} from './mockHealthTree';
import type { HealthStatus, HealthTreeDataNode, HealthTreeNode } from './mockHealthTree';

const statusOrder: HealthStatus[] = ['success', 'warning', 'danger', 'disabled'];

const statusLabels: Record<HealthStatus, string> = {
  success: 'Healthy',
  warning: 'Warning',
  danger: 'Critical',
  disabled: 'Disabled',
};

const statusDescription: Record<HealthStatus, string> = {
  success: 'Operating within the expected band.',
  warning: 'Requires attention before it turns into an outage.',
  danger: 'Actively impacting the cluster and needs intervention.',
  disabled: 'Not currently evaluated or intentionally turned off.',
};

const ModuleContent = styled.div(
  ({ theme }) => css`
    padding-top: ${theme.spacings.md};

    &::before {
      content: '';
      display: block;
      border-top: 1px solid ${theme.colors.variant.lighter.default};
    }
  `,
);

const ModuleLayout = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: minmax(300px, 340px) minmax(0, 1fr);
    min-height: 580px;

    @media (max-width: ${theme.breakpoints.max.md}) {
      grid-template-columns: 1fr;
      min-height: initial;
    }
  `,
);

const TreePane = styled.div(
  ({ theme }) => css`
    padding-top: ${theme.spacings.md};
    padding-right: ${theme.spacings.md};
    margin-right: ${theme.spacings.md};
    border-right: 1px solid ${theme.colors.variant.lighter.default};
    max-height: 640px;
    overflow: auto;

    @media (max-width: ${theme.breakpoints.max.md}) {
      padding-top: ${theme.spacings.md};
      padding-right: 0;
      margin-right: 0;
      padding-bottom: ${theme.spacings.md};
      margin-bottom: ${theme.spacings.md};
      border-right: 0;
      border-bottom: 1px solid ${theme.colors.variant.lighter.default};
      max-height: 420px;
    }
  `,
);

const DetailsPane = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
    padding-top: ${theme.spacings.md};
  `,
);

const InterpretationPane = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
    padding-top: ${theme.spacings.md};
  `,
);

const InterpretationTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
    line-height: 1.2;
  `,
);

const LegendList = styled.ul(
  ({ theme }) => css`
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.sm};
  `,
);

const LegendItem = styled.li(
  ({ theme }) => css`
    display: flex;
    align-items: flex-start;
    gap: ${theme.spacings.sm};
    line-height: 1.5;
  `,
);

const LegendText = styled.span(
  () => css`
    min-width: 0;

    strong {
      font-weight: 600;
    }
  `,
);

const StyledTree = styled(Tree)(
  ({ theme }) => css`
    margin: 0;
    padding: 0;

    .mantine-Tree-node {
      list-style: none;
    }

    .mantine-Tree-subtree {
      margin: 0;
      padding-top: ${theme.spacings.xxs};
      padding-bottom: ${theme.spacings.xxs};
    }
  `,
);

const TreeRow = styled.div<{ $selected: boolean }>(
  ({ $selected, theme }) => css`
    width: 100%;
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    padding-top: ${theme.spacings.xxs};
    padding-right: ${theme.spacings.xs};
    padding-bottom: ${theme.spacings.xxs};
    border-radius: 10px;
    min-height: 34px;
    cursor: pointer;
    color: ${theme.colors.text.primary};
    background-color: ${$selected ? theme.colors.variant.lightest.default : 'transparent'};
    box-shadow: ${$selected ? `inset 0 0 0 1px ${theme.colors.variant.lighter.default}` : 'none'};
    transition: background-color 120ms ease, box-shadow 120ms ease;

    &:hover {
      background-color: ${$selected ? theme.colors.variant.lightest.default : theme.colors.table.row.backgroundHover};
    }

    &:focus-visible {
      outline: 2px solid ${theme.colors.variant.info};
      outline-offset: 2px;
    }
  `,
);

const TreeLabel = styled.span<{ $emphasized: boolean }>(
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

const ChevronSlot = styled.span(
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

const Breadcrumbs = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
    line-height: 1.4;
  `,
);

const DetailsTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h2};
    line-height: 1.15;
  `,
);

const StatusSummary = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

const BodyText = styled.p(
  ({ theme }) => css`
    margin: 0;
    color: ${theme.colors.text.primary};
    line-height: 1.55;
  `,
);

const DetailSection = styled.section(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    padding-top: ${theme.spacings.sm};
    border-top: 1px solid ${theme.colors.variant.lighter.default};

    h4 {
      margin: 0;
      font-size: ${theme.fonts.size.body};
      line-height: 1.3;
    }
  `,
);

const ChipList = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.xs};
  `,
);

const ItemChip = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    padding: ${theme.spacings.xxs} ${theme.spacings.xs};
    border-radius: 999px;
    background-color: ${theme.colors.variant.lightest.default};
    border: 1px solid ${theme.colors.variant.lighter.default};
    font-size: ${theme.fonts.size.small};
    line-height: 1.2;
  `,
);

const ChildList = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
  `,
);

const ChildButton = styled.button`
  ${({ theme }) => css`
    width: 100%;
    border: 1px solid ${theme.colors.variant.lighter.default};
    background-color: ${theme.colors.global.contentBackground};
    border-radius: 10px;
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

const ChildButtonMeta = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: flex-start;
    gap: ${theme.spacings.sm};
    min-width: 0;
  `,
);

const ChildButtonText = styled.div(
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

const countLeaves = (node: HealthTreeNode): number => {
  if (!node.children?.length) {
    return 1;
  }

  return node.children.reduce((sum, child) => sum + countLeaves(child), 0);
};

const formatCheckCount = (count: number) => `${count} nested check${count === 1 ? '' : 's'}`;

const getNodeDetails = (value: string | undefined) => mockHealthTreeLookup[value ?? mockHealthTree.value] ?? mockHealthTree;

const getStatusMeta = (status: HealthStatus) => ({
  label: statusLabels[status],
  description: statusDescription[status],
});

const HealthModule = () => {
  const tree = useTree({
    initialExpandedState: mockHealthTreeInitialExpandedState,
    initialSelectedState: [mockHealthTree.value],
  });

  const selectedValue = tree.selectedState[0] ?? mockHealthTree.value;
  const selectedNode = getNodeDetails(selectedValue);
  const selectedStatus = getStatusMeta(selectedNode.status);
  const isInterpretationView = selectedNode.value === mockHealthTree.value;

  return (
    <Row className="content">
      <Col md={12}>
        <h2>Health of Graylog Deployment</h2>

        <ModuleContent>
          <ModuleLayout>
            <TreePane>
              <StyledTree
                aria-label="Cluster health tree"
                data={mockHealthTreeData}
                tree={tree}
                expandOnClick
                selectOnClick
                renderNode={({ expanded, hasChildren, node, selected, elementProps }) => {
                  const nodeProps = (node as HealthTreeDataNode).nodeProps;

                  return (
                    <TreeRow {...elementProps} $selected={selected}>
                      <ChevronSlot>
                        {hasChildren ? (
                          <Icon name={expanded ? 'keyboard_arrow_down' : 'keyboard_arrow_right'} size="sm" />
                        ) : null}
                      </ChevronSlot>
                      <HealthStatusIcon status={nodeProps.status} title={statusLabels[nodeProps.status]} />
                      <TreeLabel $emphasized={hasChildren}>{node.label}</TreeLabel>
                    </TreeRow>
                  );
                }} />
            </TreePane>

            {!isInterpretationView ? (
              <DetailsPane>
                <Breadcrumbs>{selectedNode.path.join(' / ')}</Breadcrumbs>
                <DetailsTitle>{selectedNode.label}</DetailsTitle>

                <StatusSummary>
                  <HealthStatusIcon status={selectedNode.status} title={selectedStatus.label} />
                  <span>{selectedStatus.label}. {selectedStatus.description}</span>
                </StatusSummary>

                <BodyText>{selectedNode.description}</BodyText>

                {selectedNode.suggestedAction && (
                  <DetailSection>
                    <h4>Suggested Action</h4>
                    <BodyText>{selectedNode.suggestedAction}</BodyText>
                  </DetailSection>
                )}

                {selectedNode.affectedNodes.length > 0 && (
                  <DetailSection>
                    <h4>Affected Entities</h4>
                    <ChipList>
                      {selectedNode.affectedNodes.map((entity) => (
                        <ItemChip key={entity}>{entity}</ItemChip>
                      ))}
                    </ChipList>
                  </DetailSection>
                )}

                {selectedNode.children?.length ? (
                  <DetailSection>
                    <h4>Contained Checks</h4>
                    <ChildList>
                      {selectedNode.children.map((child) => (
                        <ChildButton
                          key={child.value}
                          type="button"
                          onClick={() => {
                            tree.select(child.value);

                            if (child.children?.length) {
                              tree.expand(child.value);
                            }
                          }}>
                          <ChildButtonMeta>
                            <HealthStatusIcon status={child.status} title={statusLabels[child.status]} />
                            <ChildButtonText>
                              <strong>{child.label}</strong>
                              <span>{formatCheckCount(countLeaves(child))}</span>
                            </ChildButtonText>
                          </ChildButtonMeta>
                          <Icon name="keyboard_arrow_right" size="sm" />
                        </ChildButton>
                      ))}
                    </ChildList>
                  </DetailSection>
                ) : null}
              </DetailsPane>
            ) : (
              <InterpretationPane>
                <InterpretationTitle>How to interpret this health report:</InterpretationTitle>
                <BodyText>
                  This health report groups checks by subsystem. Select any branch or check on the left to review its root cause, suggested action, and affected entities. Every check resolves to one of four states:
                </BodyText>
                <LegendList>
                  {statusOrder.map((status) => (
                    <LegendItem key={status}>
                      <HealthStatusIcon status={status} title={statusLabels[status]} />
                      <LegendText>
                        <strong>{statusLabels[status]}:</strong> {statusDescription[status]}
                      </LegendText>
                    </LegendItem>
                  ))}
                </LegendList>
              </InterpretationPane>
            )}
          </ModuleLayout>
        </ModuleContent>
      </Col>
    </Row>
  );
};

export default HealthModule;
