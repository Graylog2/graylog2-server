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

import { Row, Col, Button } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import type { RuleBlock, RuleBuilderType } from 'hooks/useRuleBuilder';

import RuleBuilderBlock from './RuleBuilderBlock';

type Props = {
  isNewRule: boolean,
};

const RuleBuilder = ({ isNewRule }: Props) => {
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
  } = useRuleBuilder(isNewRule);

  console.log('rule', rule);
  console.log('conditionsDict', conditionsDict);
  console.log('actionsDict', actionsDict);

  const initialRuleBuilder = rule?.rule_builder || { errors: [], conditions: [], actions: [] };

  const [ruleBuilder, setRuleBuilder] = useState<RuleBuilderType>(initialRuleBuilder);
  const [showNewConditionBlock, setShowNewConditionBlock] = useState<boolean>(false);
  const [showNewActionBlock, setShowNewActionBlock] = useState<boolean>(false);

  const newConditionBlockOrder = ruleBuilder?.conditions.length || 0;
  const newActionBlockOrder = ruleBuilder?.actions.length || 0;

  // const validateRuleBuilder = () => fetchValidateRule({ ...rule, rule_builder: ruleBuilder }).then((result) => {
  //   setRuleBuilder(result.rule_builder);
  // });

  const addBlock = async (type: string, block: RuleBlock) => {
    // validateRuleBuilder();

    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      setRuleBuilder({ ...ruleBuilder, conditions: [...ruleBuilder.conditions, block] });
    } else {
      setRuleBuilder({ ...ruleBuilder, actions: [...ruleBuilder.actions, block] });
    }
  };

  const updateBlock = async (orderIndex: number, type: string, block: RuleBlock) => {
    // validateRuleBuilder();
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

    // validateRuleBuilder();
  };

  return (
    <Row className="content">
      <Col md={6}>
        {
          ruleBuilder?.conditions.map((condition, index) => (
            <RuleBuilderBlock blockDict={conditionsDict}
                              block={condition}
                              order={index}
                              type="condition"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock} />
          ))
        }
        {(showNewConditionBlock || !newConditionBlockOrder) && (
          <RuleBuilderBlock blockDict={conditionsDict || []}
                            block={null}
                            order={newConditionBlockOrder}
                            type="condition"
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={deleteBlock} />
        )}
        {(newConditionBlockOrder > 0) && (
          <Button bsStyle="info" onClick={() => setShowNewConditionBlock(true)}>Add Condition</Button>
        )}
      </Col>
      <Col md={6}>
        {
          ruleBuilder?.actions.map((action, index) => (
            <RuleBuilderBlock blockDict={actionsDict}
                              block={action}
                              order={index}
                              type="action"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock} />
          ))
        }
        {(showNewActionBlock || !newActionBlockOrder) && (
          <RuleBuilderBlock blockDict={actionsDict || []}
                            block={null}
                            order={newActionBlockOrder}
                            type="action"
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={deleteBlock} />
        )}
        {(newActionBlockOrder > 0) && (
          <Button bsStyle="info" onClick={() => setShowNewActionBlock(true)}>Add Action</Button>
        )}
      </Col>
    </Row>
  );
};

export default RuleBuilder;
