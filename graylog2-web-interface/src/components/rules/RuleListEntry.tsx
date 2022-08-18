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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import { LinkContainer, Link } from 'components/common/router';
import { MetricContainer, CounterRate } from 'components/metrics';
import { RelativeTime, OverlayTrigger, CountBadge } from 'components/common';
import { Button, ButtonToolbar, Tooltip } from 'components/bootstrap';
import Routes from 'routing/Routes';
import type { RuleType, PipelineSummary } from 'stores/rules/RulesStore';
import StringUtils from 'util/StringUtils';

type Props = {
  rule: RuleType,
  usingPipelines: Array<PipelineSummary>
  onDelete: (rule: RuleType) => () => void,
}
const STRING_SIZE_LIMIT = 30;
const LimitedTd = styled.td(({ theme }: {theme: DefaultTheme}) => css`
  max-width: 250px;
  min-width: 250px;
  
  @media screen and (max-width: ${theme.breakpoints.max.md}) {
    white-space: normal !important;
  }
`);

const RuleListEntry = ({ rule, onDelete, usingPipelines }: Props) => {
  const { id, title, description, created_at, modified_at } = rule;
  const pipelinesLength = usingPipelines.length;
  const actions = (
    <ButtonToolbar>
      <Button bsStyle="primary" bsSize="xsmall" onClick={onDelete(rule)} title="Delete rule">
        Delete
      </Button>
      <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE(id)}>
        <Button bsStyle="info" bsSize="xsmall">Edit</Button>
      </LinkContainer>
    </ButtonToolbar>
  );

  const _showPipelines = (pipelines: Array<PipelineSummary>) => {
    return pipelines.map(({ id: pipelineId, title: pipelineTitle }, index) => {
      const tooltip = <Tooltip id={`${id}${pipelineId}`} show>{pipelineTitle}</Tooltip>;

      return (
        <React.Fragment key={pipelineId}>
          {pipelineTitle.length > STRING_SIZE_LIMIT ? (
            <OverlayTrigger placement="top" trigger="hover" overlay={tooltip} rootClose>
              <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipelineId)}>
                {StringUtils.truncateWithEllipses(pipelineTitle, STRING_SIZE_LIMIT)}
              </Link>
            </OverlayTrigger>
          ) : (
            <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipelineId)}>
              {pipelineTitle}
            </Link>
          )}
          {index < (pipelinesLength - 1) && ',  '}
        </React.Fragment>
      );
    });
  };

  return (
    <tr key={title}>
      <td>
        <Link to={Routes.SYSTEM.PIPELINES.RULE(id)}>
          {title}
        </Link>
      </td>
      <td className="limited">{description}</td>
      <td className="limited"><RelativeTime dateTime={created_at} /></td>
      <td className="limited"><RelativeTime dateTime={modified_at} /></td>
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
      <LimitedTd>
        <CountBadge>{pipelinesLength}</CountBadge>
        {' '}
        {_showPipelines(usingPipelines)}
      </LimitedTd>
      <td className="actions">{actions}</td>
    </tr>
  );
};

export default RuleListEntry;
