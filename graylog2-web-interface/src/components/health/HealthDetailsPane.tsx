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
import type { useTree } from '@mantine/core';

import { Button } from 'components/bootstrap';
import { ExternalLink, Icon, LinkContainer } from 'components/common';

import HealthStatusIcon from './HealthStatusIcon';
import { countContainedChecks, formatLeafCountVerbose } from './healthTree';
import { getStatusMeta, STATUS_LABELS } from './healthStatusCopy';
import HEALTH_CHECK_DEFINITIONS, { getEntityListFor } from './healthCheckDefinitions';
import type { HealthNode } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';
import {
  BodyText,
  Breadcrumbs,
  CauseList,
  ChildButton,
  ChildButtonMeta,
  ChildButtonText,
  ChildCountSuffix,
  ChildList,
  DetailsPane,
  DetailSection,
  DetailsTitle,
  MessageBlock,
  StatusSummary,
} from './HealthModule.styles';

type Props = {
  tree: ReturnType<typeof useTree>;
  selectedNode: HealthNode;
  selectedPath: string[];
};

const formatCheckCount = (count: number) => `${count} check${count === 1 ? '' : 's'}`;

const AffectedChildButton = ({ child, tree }: { child: HealthNode; tree: ReturnType<typeof useTree> }) => {
  const childIsFeature = isHealthFeature(child);
  const childCountSummary = formatLeafCountVerbose(child, getEntityListFor(child.id)?.label);

  const handleClick = () => {
    tree.select(child.id);

    if (childIsFeature) tree.expand(child.id);
  };

  return (
    <ChildButton type="button" onClick={handleClick}>
      <ChildButtonMeta>
        <HealthStatusIcon status={child.status} title={STATUS_LABELS[child.status]} />
        <ChildButtonText>
          <strong>
            {child.title}
            {childCountSummary ? <ChildCountSuffix> ({childCountSummary})</ChildCountSuffix> : null}
          </strong>
          {childIsFeature ? <span>{formatCheckCount(countContainedChecks(child))}</span> : null}
        </ChildButtonText>
      </ChildButtonMeta>
      <Icon name="keyboard_arrow_right" size="sm" />
    </ChildButton>
  );
};

// `<div>` wrapper prevents the button from stretching to fill the parent flex column.
const EntityListButton = ({ url, label }: { url: string; label: string }) => (
  <div>
    <LinkContainer to={url}>
      <Button bsStyle="primary" bsSize="small">
        View {label} <Icon name="arrow_right_alt" />
      </Button>
    </LinkContainer>
  </div>
);

const HealthDetailsPane = ({ tree, selectedNode, selectedPath }: Props) => {
  const status = selectedNode.status;
  const definition = HEALTH_CHECK_DEFINITIONS[selectedNode.id];
  const description = definition?.description;
  const entityList = getEntityListFor(selectedNode.id);
  const statusMeta = getStatusMeta(status);

  const isFeatureWithChildren = isHealthFeature(selectedNode) && selectedNode.children.length > 0;
  const isLeafNode = !isFeatureWithChildren;
  const isUnhealthy = status !== 'healthy';
  const countSummary = formatLeafCountVerbose(selectedNode, entityList?.label);

  const meaning = isLeafNode && isUnhealthy ? definition?.meaning : undefined;
  const latestMessage = isUnhealthy && selectedNode.message?.trim() ? selectedNode.message : undefined;
  // Backend specifics supersede generic causes — when a message is present, hide commonCauses.
  const commonCauses = isLeafNode && isUnhealthy && !latestMessage ? definition?.commonCauses : undefined;
  const recommendedAction = isLeafNode && isUnhealthy ? definition?.recommendedAction : undefined;
  const docsUrl = isLeafNode ? definition?.docsUrl : undefined;

  const affectedChildren = isFeatureWithChildren
    ? selectedNode.children.filter((child) => child.status !== 'healthy')
    : [];

  return (
    <DetailsPane>
      <Breadcrumbs>{selectedPath.join(' / ')}</Breadcrumbs>
      <DetailsTitle>{selectedNode.title}</DetailsTitle>

      <StatusSummary>
        <HealthStatusIcon status={status} title={statusMeta.label} />
        <span>
          {statusMeta.label}. {statusMeta.description}
          {countSummary ? ` · (${countSummary})` : null}
        </span>
      </StatusSummary>

      {description ? <BodyText>{description}</BodyText> : null}

      {affectedChildren.length > 0 ? (
        <DetailSection>
          <h4>Affected</h4>
          <ChildList>
            {affectedChildren.map((child) => (
              <AffectedChildButton key={child.id} child={child} tree={tree} />
            ))}
          </ChildList>
        </DetailSection>
      ) : null}

      {meaning ? (
        <DetailSection>
          <h4>What this means</h4>
          <BodyText>{meaning}</BodyText>
        </DetailSection>
      ) : null}

      {commonCauses?.length ? (
        <DetailSection>
          <h4>Common causes</h4>
          <CauseList>
            {commonCauses.map((cause) => (
              <li key={cause}>{cause}</li>
            ))}
          </CauseList>
        </DetailSection>
      ) : null}

      {recommendedAction ? (
        <DetailSection>
          <h4>Recommended action</h4>
          <BodyText>{recommendedAction}</BodyText>
        </DetailSection>
      ) : null}

      {isLeafNode && entityList ? <EntityListButton url={entityList.url} label={entityList.label} /> : null}

      {latestMessage ? (
        <DetailSection>
          <h4>Latest message</h4>
          <MessageBlock>{latestMessage}</MessageBlock>
        </DetailSection>
      ) : null}

      {docsUrl ? (
        <DetailSection>
          <BodyText>
            <ExternalLink href={docsUrl}>Learn more</ExternalLink>
          </BodyText>
        </DetailSection>
      ) : null}
    </DetailsPane>
  );
};

export default HealthDetailsPane;
