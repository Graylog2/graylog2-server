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
import { useState } from 'react';
import styled from 'styled-components';

import { LinkContainer, Link } from 'components/common/router';
import { MetricContainer, CounterRate } from 'components/metrics';
import { Timestamp, OverlayTrigger } from 'components/common';
import { Button, Label, Tooltip } from 'components/bootstrap';
import Routes from 'routing/Routes';
import type { RuleType, UsingPipeline } from 'stores/rules/RulesStore';
import StringUtils from 'util/StringUtils';

type Props = {
  rule: RuleType,
  usingPipelines: Array<UsingPipeline>
  onDelete: (rule: RuleType) => void,
}
const Pipeline = styled(Label)`
  margin-right: 5px;
  margin-bottom: 5px;
  display: inline-block;
  line-height: 15px;
`;

const RuleListEntry = ({ rule, onDelete, usingPipelines }: Props) => {
  const [viewUsingPipelines, setViewUsingPipelines] = useState(false);
  const { id, title, description, created_at, modified_at } = rule;
  const actions = [
    <Button key="delete" bsStyle="primary" bsSize="xsmall" onClick={onDelete(rule)} title="Delete rule">
      Delete
    </Button>,
    <span key="space">&nbsp;</span>,
    <LinkContainer key="edit" to={Routes.SYSTEM.PIPELINES.RULE(id)}>
      <Button bsStyle="info" bsSize="xsmall">Edit</Button>
    </LinkContainer>,
  ];
  const _togglePipelinesDetails = () => setViewUsingPipelines(!viewUsingPipelines);

  const _showPipelines = (pipelines: Array<UsingPipeline>) => {
    return pipelines.map(({ id: pipelineId, title: pipelineTitle }) => {
      const tooltip = <Tooltip id={StringUtils.replaceSpaces(pipelineTitle)} show>{pipelineTitle}</Tooltip>;

      return (
        <OverlayTrigger key={pipelineId} placement="top" trigger="hover" overlay={tooltip} rootClose>
          <LinkContainer key="view" to={Routes.SYSTEM.PIPELINES.PIPELINE(pipelineId)}>
            <Pipeline bsStyle="default">{StringUtils.truncateWithEllipses(pipelineTitle, 30)}</Pipeline>
          </LinkContainer>
        </OverlayTrigger>
      );
    });
  };

  return (
    <tr key={title} onClick={_togglePipelinesDetails}>
      <td>
        <Link to={Routes.SYSTEM.PIPELINES.RULE(id)}>
          {title}
        </Link>
      </td>
      <td className="limited">{description}</td>
      <td className="limited"><Timestamp dateTime={created_at} relative /></td>
      <td className="limited"><Timestamp dateTime={modified_at} relative /></td>
      <td>
        <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${id}.executed`} zeroOnMissing>
          <CounterRate suffix="msg/s" />
        </MetricContainer>
      </td>
      <td>
        <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${id}.failed`}>
          <CounterRate showTotal suffix="errors/s" hideOnMissing />
        </MetricContainer>
      </td>
      <td className="limited">{_showPipelines(usingPipelines)}</td>
      <td className="actions">{actions}</td>
    </tr>
  );
};

export default RuleListEntry;
