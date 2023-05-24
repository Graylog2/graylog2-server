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
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

import useHistory from 'routing/useHistory';
import { Row, Col, Button } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import { FormSubmit, ConfirmDialog } from 'components/common';

import RuleBuilderBlock from './RuleBuilderBlock';
import RuleBuilderForm from './RuleBuilderForm';
import type { BlockType, RuleBlock, RuleBuilderRule } from './types';
import { getDictForFunction, getDictForParam } from './helpers';

import RuleSimulation from '../RuleSimulation';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const SubTitle = styled.label`
  color: #aaa;
`;

const RuleBuilder = () => {
  const {
    rule: initialRule,
    conditionsDict,
    actionsDict,
    createRule,
    updateRule,
    // fetchValidateRule,
  } = useRuleBuilder();

  const [rule, setRule] = useState<RuleBuilderRule>({ description: '', title: '', rule_builder: { conditions: [], actions: [] } });
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);

  useEffect(() => {
    if (initialRule) {
      setRule(initialRule);
    }
  }, [initialRule]);

  const history = useHistory();

  console.log('conditionsDict', conditionsDict);
  console.log('actionsDict', actionsDict);
  console.log('initialRule', initialRule);
  console.log('currentRule', rule);

  const newConditionBlockOrder = rule.rule_builder.conditions.length;
  const newActionBlockOrder = rule.rule_builder.actions.length;

  // const validateRuleBuilder = () => fetchValidateRule({ ...rule, rule_builder: ruleBuilder }).then((result) => {
  //   setRuleBuilder(result.rule_builder);
  // });

  // TODO: only for actions
  // return_type Javalang.void -> no output variable (already in backend validation)

  // TODO: Set primary params and output variables again on delete and reorder

  const setPrimaryParams = (block: RuleBlock): RuleBlock => {
    const blockDict = getDictForFunction(actionsDict, block.function);

    const isParamSet = (paramName) => (
      block.params[paramName] && block.params[paramName] !== '' && block.params[paramName] !== null
    );

    if (!blockDict) return block;

    const newBlock = block;

    Object.keys(block.params).forEach((paramName) => {
      if (getDictForParam(blockDict, paramName)?.primary && (!isParamSet(paramName))) {
        const lastBlock = rule.rule_builder.actions[rule.rule_builder.actions.length - 1];

        if (!lastBlock?.outputvariable) { return; }

        newBlock.params[paramName] = `$${lastBlock.outputvariable}`;
      }
    });

    return newBlock;
  };

  const addBlock = async (type: BlockType, block: RuleBlock) => {
    // validateRuleBuilder();
    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          conditions: [...rule.rule_builder.conditions,
            { ...block },
          ],
        },
      });
    } else {
      const blockToSet = setPrimaryParams(block);

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          actions: [...rule.rule_builder.actions,
            { ...blockToSet, outputvariable: `output_actions_${newActionBlockOrder + 1}` },
          ],
        },
      });
    }
  };

  const updateBlock = async (orderIndex: number, type: string, block: RuleBlock) => {
    // validateRuleBuilder();
    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      const currentConditions = [...rule.rule_builder.conditions];

      currentConditions[orderIndex] = block;

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, conditions: currentConditions,
        },
      });
    } else {
      const blockToSet = setPrimaryParams(block);

      const currentActions = [...rule.rule_builder.actions];
      currentActions[orderIndex] = blockToSet;

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      });
    }
  };

  const deleteBlock = async (orderIndex: number, type: BlockType) => {
    if (type === 'condition') {
      const currentConditions = [...rule.rule_builder.conditions];
      currentConditions.splice(orderIndex, 1);

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, conditions: currentConditions,
        },
      });
    } else {
      const currentActions = [...rule.rule_builder.actions];
      currentActions.splice(orderIndex, 1);

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      });
    }

    // validateRuleBuilder();
  };

  const handleCancel = () => {
    history.goBack();
  };

  const handleSave = async (event?: React.FormEvent<HTMLFormElement>, closeAfter: boolean = false) => {
    event?.preventDefault();

    if (initialRule) {
      await updateRule(rule);
      if (closeAfter) handleCancel();
    } else {
      await createRule(rule);
    }
  };

  return (
    <form onSubmit={(e) => handleSave(e, true)}>
      <Row className="content">
        <Col md={12}>
          <RuleBuilderForm rule={rule}
                           onChange={setRule} />
          <label htmlFor="rule_builder">Rule Builder</label>
        </Col>
        <Col md={6}>
          <SubTitle htmlFor="rule_builder_conditions">Conditions</SubTitle>
          {rule.rule_builder.conditions.map((condition, index) => (
            // eslint-disable-next-line react/no-array-index-key
            <RuleBuilderBlock key={index}
                              blockDict={conditionsDict || []}
                              block={condition}
                              order={index}
                              type="condition"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: index, type: 'condition' })} />
          ))}
          <RuleBuilderBlock blockDict={conditionsDict || []}
                            block={null}
                            order={newConditionBlockOrder}
                            type="condition"
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={() => setBlockToDelete({ orderIndex: newConditionBlockOrder, type: 'condition' })} />
        </Col>
        <Col md={6}>
          <SubTitle htmlFor="rule_builder_actions">Actions</SubTitle>
          {rule.rule_builder.actions.map((action, index) => (
            // eslint-disable-next-line react/no-array-index-key
            <RuleBuilderBlock key={index}
                              blockDict={actionsDict || []}
                              block={action}
                              order={index}
                              type="action"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: index, type: 'action' })} />
          ))}
          <RuleBuilderBlock blockDict={actionsDict || []}
                            block={null}
                            order={newActionBlockOrder}
                            type="action"
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={() => setBlockToDelete({ orderIndex: newActionBlockOrder, type: 'action' })} />
        </Col>
        <Col md={12}>
          <RuleSimulation rule={rule} />
        </Col>
        <ActionsCol md={12}>
          <FormSubmit submitButtonText={`${!initialRule ? 'Create rule' : 'Update rule & close'}`}
                      centerCol={initialRule && (
                        <Button type="button" bsStyle="info" onClick={handleSave}>
                          Update rule
                        </Button>
                      )}
                      onCancel={handleCancel} />
        </ActionsCol>
      </Row>
      {blockToDelete && (
        <ConfirmDialog title={`Delete ${blockToDelete.type}`}
                       show
                       onConfirm={() => {
                         deleteBlock(blockToDelete.orderIndex, blockToDelete.type);
                         setBlockToDelete(null);
                       }}
                       onCancel={() => setBlockToDelete(null)}>
          <>Are you sure you want to delete <strong>{blockToDelete.type} N° {blockToDelete.orderIndex + 1}</strong>?</>
        </ConfirmDialog>
      )}
    </form>
  );
};

export default RuleBuilder;
