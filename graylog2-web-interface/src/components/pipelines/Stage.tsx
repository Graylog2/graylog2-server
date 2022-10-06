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
import PropTypes from 'prop-types';
import React from 'react';

import { Col, Button } from 'components/bootstrap';
import { EntityListItem, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import type { PipelineType, StageType } from 'stores/pipelines/PipelinesStore';
import { useStore } from 'stores/connect';
import { RulesStore } from 'stores/rules/RulesStore';
import type { RuleType } from 'stores/rules/RulesStore';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

import StageForm from './StageForm';
import StageRules from './StageRules';

type Props = {
  stage: StageType,
  pipeline: PipelineType,
  isLastStage: boolean,
  onUpdate: (nextStage: StageType, callback: () => void) => void,
  onDelete: () => void,
};

const Stage = ({ stage, pipeline, isLastStage, onUpdate, onDelete }: Props) => {
  const currentUser = useCurrentUser();
  const { rules: allRules }: { rules: RuleType[] } = useStore(RulesStore);

  const suffix = `Contains ${(stage.rules.length === 1 ? '1 rule' : `${stage.rules.length} rules`)}`;

  const throughput = (
    <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.stage.${stage.stage}.executed`}>
      <CounterRate showTotal={false} prefix="Throughput: " suffix="msg/s" />
    </MetricContainer>
  );

  const actions = [
    <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:edit')} key={`delete-stage-${stage}`} bsStyle="primary" onClick={onDelete}>Delete</Button>,
    <StageForm key={`edit-stage-${stage}`} pipeline={pipeline} stage={stage} save={onUpdate} />,
  ];

  let description;

  if (isLastStage) {
    description = 'There are no further stages in this pipeline. Once rules in this stage are applied, the pipeline will have finished processing.';
  } else {
    let matchText;

    switch (stage.match) {
      case 'ALL':
        matchText = 'all rules';
        break;
      case 'EITHER':
        matchText = 'at least one rule';
        break;
      case 'PASS':
        matchText = 'none or more rules';
        break;
      default:
        matchText = 'UNKNOWN';
        break;
    }

    description = (
      <span>
        Messages satisfying <strong>{matchText}</strong>{' '}
        in this stage, will continue to the next stage.
      </span>
    );
  }

  const block = (
    <span>
      {description}
      <br />
      {throughput}
    </span>
  );

  const content = (allRules
    ? (
      <StageRules pipeline={pipeline}
                  stage={stage}
                  rules={stage.rules.map((name) => allRules.filter((r) => r.title === name)[0])} />
    )
    : <Spinner />);

  return (
    <EntityListItem title={`Stage ${stage.stage}`}
                    titleSuffix={suffix}
                    actions={actions}
                    description={block}
                    contentRow={<Col md={12}>{content}</Col>} />
  );
};

Stage.propTypes = {
  stage: PropTypes.object.isRequired,
  pipeline: PropTypes.object.isRequired,
  isLastStage: PropTypes.bool.isRequired,
  onUpdate: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
};

export default Stage;
