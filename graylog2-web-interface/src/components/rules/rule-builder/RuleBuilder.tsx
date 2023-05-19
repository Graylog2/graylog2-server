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
import styled from 'styled-components';

import useHistory from 'routing/useHistory';
import { Row, Col, Button } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import { FormSubmit, ConfirmDialog } from 'components/common';

import RuleBuilderBlock from './RuleBuilderBlock';
import RuleBuilderForm from './RuleBuilderForm';
import type { RuleBlock, RuleBuilderRule } from './types';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const AddButton = styled(Button)`
  margin-top: 20px;
`;

const SubTitle = styled.label`
  color: #aaa;
`;

const RuleBuilder = () => {
  const {
    rule: existingRule,
    conditionsDict,
    actionsDict,
    createRule,
    updateRule,
    // fetchValidateRule,
  } = useRuleBuilder();

  const history = useHistory();

  const initialRule = existingRule || { description: '', title: '', rule_builder: { conditions: [], actions: [] } };

  const [rule, setRule] = useState<RuleBuilderRule>(initialRule);
  const [showNewConditionBlock, setShowNewConditionBlock] = useState<boolean>(false);
  const [showNewActionBlock, setShowNewActionBlock] = useState<boolean>(false);
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: 'condition'|'action' } | null>(null);

  console.log('conditionsDict', conditionsDict);
  console.log('actionsDict', actionsDict);
  console.log('initialRule', initialRule);
  console.log('currentRule', rule);

  const newConditionBlockOrder = rule.rule_builder.conditions.length;
  const newActionBlockOrder = rule.rule_builder.actions.length;

  // const validateRuleBuilder = () => fetchValidateRule({ ...rule, rule_builder: ruleBuilder }).then((result) => {
  //   setRuleBuilder(result.rule_builder);
  // });

  const addBlock = async (type: string, block: RuleBlock) => {
    // validateRuleBuilder();
    const isValid = true;

    if (!isValid) return;

    if (type === 'condition') {
      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, conditions: [...rule.rule_builder.conditions, block],
        },
      });
    } else {
      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: [...rule.rule_builder.actions, block],
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
      const currentActions = [...rule.rule_builder.actions];
      currentActions[orderIndex] = block;

      setRule({
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      });
    }
  };

  const deleteBlock = async (orderIndex: number, type: 'condition'|'action') => {
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

  const handleSave = async (event?: React.FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (existingRule) {
      await updateRule(rule);
      handleCancel();
    } else {
      await createRule(rule);
    }
  };

  return (
    <form onSubmit={handleSave}>
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
          {(showNewConditionBlock || !newConditionBlockOrder) && (
            <RuleBuilderBlock blockDict={conditionsDict || []}
                              block={null}
                              order={newConditionBlockOrder}
                              type="condition"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: newConditionBlockOrder, type: 'condition' })} />
          )}
          {(newConditionBlockOrder > 0) && (
            <AddButton bsSize="small" bsStyle="info" onClick={() => setShowNewConditionBlock(true)}>Add Condition</AddButton>
          )}
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
          {(showNewActionBlock || !newActionBlockOrder) && (
            <RuleBuilderBlock blockDict={actionsDict || []}
                              block={null}
                              order={newActionBlockOrder}
                              type="action"
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: newActionBlockOrder, type: 'action' })} />
          )}
          {(newActionBlockOrder > 0) && (
            <AddButton bsSize="small" bsStyle="info" onClick={() => setShowNewActionBlock(true)}>Add Action</AddButton>
          )}
        </Col>
        <ActionsCol md={12}>
          <FormSubmit submitButtonText={`${!existingRule ? 'Create rule' : 'Update rule & close'}`}
                      centerCol={existingRule && (
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
