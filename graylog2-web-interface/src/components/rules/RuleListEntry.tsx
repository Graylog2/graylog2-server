import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';

import { LinkContainer, Link } from 'components/graylog/router';
import { MetricContainer, CounterRate } from 'components/metrics';
import { Timestamp } from 'components/common';
import { Button, Label, Tooltip, OverlayTrigger } from 'components/graylog';
import Routes from 'routing/Routes';
import type { RuleType } from 'stores/rules/RulesStore';
import StringUtils from 'util/StringUtils';

type Props = {
  rule: RuleType,
  onDelete: (rule: RuleType) => void,
}
const Pipeline = styled(Label)`
  margin-right: 5px;
  margin-bottom: 5px;
  display: inline-block;
  line-height: 15px;
`;


const RuleListEntry = ({ rule, onDelete }: Props) => {
  const [viewUsingPipelines, setViewUsingPipelines] = useState(false);
  const { id, title, description, created_at, modified_at, using_pipelines } = rule;
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
  const _showPipelines = (usingPipelines: Array<string>) => {
    return usingPipelines.map((pipelineName) => {
      const tooltip = <Tooltip id={StringUtils.replaceSpaces(pipelineName)} show>{pipelineName}</Tooltip>;
      return (
        <OverlayTrigger key={pipelineName} placement="top" trigger="hover" overlay={tooltip} rootClose>
          <Pipeline  bsStyle="default">{StringUtils.truncateWithEllipses(pipelineName, 30)}</Pipeline>
        </OverlayTrigger>
      )
    });
  };

  return(
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
      <td className="limited">{_showPipelines(using_pipelines)}</td>
      <td className="actions">{actions}</td>
    </tr>
  );
};

export default RuleListEntry;
