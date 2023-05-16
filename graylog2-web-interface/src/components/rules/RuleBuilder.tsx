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
import React, { useContext, useState } from 'react';

import { Row, Col } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import type { RuleBlock, RuleBuilderType } from 'hooks/useRuleBuilder';

import RuleBuilderBlock from './RuleBuilderBlock';
import { PipelineRulesContext } from './RuleContext';

type Props = {
  isNewRule: boolean,
};

const RuleBuilder = ({ isNewRule }: Props) => {
  const {
    description,
    handleDescription,
    handleSavePipelineRule,
    ruleSourceRef,
    onAceLoaded,
    onChangeSource,
    ruleSource,
    simulateRule,
    rawMessageToSimulate,
    setRawMessageToSimulate,
    ruleSimulationResult,
    setRuleSimulationResult,
    startRuleSimulation,
    setStartRuleSimulation,
  } = useContext(PipelineRulesContext);

  const {
    isLoadingConditions,
    isLoadingActions,
    rule,
    conditionsDict,
    actionsDict,
    refetchConditions,
    refetchActions,
    createRule,
    updateRule,
    fetchValidateRule,
  } = useRuleBuilder();

  const [ruleBuilder, setRuleBuilder] = useState<RuleBuilderType>(rule.rule_builder);

  const validateRuleBuilder = () => fetchValidateRule({ ...rule, rule_builder: ruleBuilder }).then((result) => {
    setRuleBuilder(result.rule_builder);
  });

  const addBlock = async (type: string, block: RuleBlock) => {
    validateRuleBuilder();
    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      setRuleBuilder({ ...ruleBuilder, conditions: [...ruleBuilder.conditions, block] });
    } else {
      setRuleBuilder({ ...ruleBuilder, actions: [...ruleBuilder.actions, block] });
    }
  };

  const updateBlock = async (orderIndex: number, type: string, block: RuleBlock) => {
    validateRuleBuilder();
    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      const currentConditions = [...ruleBuilder.conditions];
      currentConditions[orderIndex] = block;

      setRuleBuilder({ ...ruleBuilder, conditions: currentConditions });
    } else {
      const currentActions = [...ruleBuilder.actions];
      currentActions[orderIndex] = block;

      setRuleBuilder({ ...ruleBuilder, actions: currentActions });
    }
  };

  const deleteBlock = (orderIndex: number, type: string) => {
    if (type === 'condition') {
      const currentConditions = [...ruleBuilder.conditions];
      currentConditions.splice(orderIndex, 1);

      setRuleBuilder({ ...ruleBuilder, conditions: currentConditions });
    } else {
      const currentActions = [...ruleBuilder.actions];
      currentActions.splice(orderIndex, 1);

      setRuleBuilder({ ...ruleBuilder, actions: currentActions });
    }

    validateRuleBuilder();
  };

  return (
    <Row className="content">
      <Col md={6}>
        {
          ruleBuilder.conditions.map((condition, index) => (
            <RuleBuilderBlock blockDict={conditionsDict}
                              block={condition}
                              order={index}
                              type="condition"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock} />
          ))
        }
      </Col>
      <Col md={6}>
        {
          ruleBuilder.actions.map((action, index) => (
            <RuleBuilderBlock blockDict={actionsDict}
                              block={action}
                              order={index}
                              type="action"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock} />
          ))
        }
      </Col>
    </Row>
  );
};

export default RuleBuilder;
