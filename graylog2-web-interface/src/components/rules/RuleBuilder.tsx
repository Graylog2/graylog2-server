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
    conditions: initialConditions,
    actions: initialActions,
    conditionsDict,
    actionsDict,
    refetchConditions,
    refetchActions,
    createRule,
    updateRule,
    fetchValidateRule,
  } = useRuleBuilder();

  const [conditions, setConditions] = useState<any[]>(initialConditions);
  const [actions, setActions] = useState<any[]>(initialActions);
  const [errors, setErrors] = useState<any[]>([]);

  const validateBlock = () => {
    return fetchValidateRule().then((errors) => {
      if (errors) {
        setErrors(errors);

        return false;
      }

      return true;
    });
  };

  const addBlock = async (type: string, block: object) => {
    const isValid = await validateBlock();

    if (!isValid) return;

    if (type === 'condition') {
      setConditions([...conditions, block]);
    } else {
      setActions([...actions, block]);
    }
  };

  const updateBlock = async (orderIndex: number, type: string, block: object) => {
    const isValid = await validateBlock();

    if (!isValid) return;

    if (type === 'condition') {
      const currentConditions = [...conditions];
      currentConditions[orderIndex] = block;

      setConditions(currentConditions);
    } else {
      const currentActions = [...actions];
      currentActions[orderIndex] = block;

      setActions(currentActions);
    }
  };

  const deleteBlock = (orderIndex: number, type: string) => {
    if (type === 'condition') {
      const currentConditions = [...conditions];
      currentConditions.splice(orderIndex, 1);

      setConditions(currentConditions);
    } else {
      const currentActions = [...actions];
      currentActions.splice(orderIndex, 1);

      setActions(currentActions);
    }

    validateBlock();
  };

  return (
    <Row className="content">
      <Col md={6}>
        {
          conditions.map((condition) => (
            <RuleBuilderBlock blockDict={conditionsDict}
                              block={condition}
                              type="condition"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock}
                              errors={errors} />
          ))
        }
      </Col>
      <Col md={6}>
        {
          actions.map((action) => (
            <RuleBuilderBlock blockDict={actionsDict}
                              block={action}
                              type="action"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={deleteBlock}
                              errors={errors} />
          ))
        }
      </Col>
    </Row>
  );
};

export default RuleBuilder;
