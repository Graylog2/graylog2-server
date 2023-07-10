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
import ObjectID from 'bson-objectid';

import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { Row, Col, Button } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import { ConfirmDialog, FormSubmit } from 'components/common';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import Errors from './Errors';
import RuleBuilderBlock from './RuleBuilderBlock';
import RuleBuilderForm from './RuleBuilderForm';
import { RULE_BUILDER_TYPES_WITH_OUTPUT } from './types';
import type { BlockType, RuleBlock, RuleBuilderRule, RuleBuilderTypes } from './types';
import {
  getDictForFunction,
  getDictForParam,
  getActionOutputVariableName,
  paramValueExists,
  paramValueIsVariable,
} from './helpers';
import ConvertToSourceCodeModal from './ConvertToSourceCodeModal';

import RuleSimulation from '../RuleSimulation';
import RuleHelper from '../rule-helper/RuleHelper';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const ReferenceRuleCol = styled(Col)`
  .ref-rule {
    height: 100px;
  }
`;

const ConvertButton = styled(Button)`
  float: right;
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
    fetchValidateRule,
  } = useRuleBuilder();

  const [rule, setRule] = useState<RuleBuilderRule>({
    description: '',
    title: '',
    rule_builder: { conditions: [], actions: [] },
  });
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const [ruleSourceCodeToShow, setRuleSourceCodeToShow] = useState<RuleBuilderRule | null>(null);

  useEffect(() => {
    if (initialRule) {
      setRule(initialRule);
    }
  }, [initialRule]);

  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const newConditionBlockIndex = rule.rule_builder.conditions.length;
  const newActionBlockIndex = rule.rule_builder.actions.length;

  const validateAndSaveRuleBuilder = (ruleToValidate: RuleBuilderRule) => fetchValidateRule(ruleToValidate).then((ruleValidated) => {
    setRule({ ...ruleToValidate, rule_builder: ruleValidated.rule_builder });
  }).catch(() => setRule(ruleToValidate));

  // TODO: Add reorder functionality (make sure to setPrimaryParamsAndOutputVariable and validate)

  const getPreviousBlock = (blocks: RuleBlock[], index?: number): RuleBlock | undefined => {
    if (index === 0) return undefined;

    if (!index) return (blocks[blocks.length - 1]);

    return (blocks[index - 1]);
  };

  const getPreviousOutput = (blocks: RuleBlock[], index?: number): {
    previousOutputPresent: boolean,
    outputVariable?: string
  } => {
    const previousBlock = getPreviousBlock(blocks, index);

    if (!previousBlock?.outputvariable) return { previousOutputPresent: false };

    return { previousOutputPresent: true, outputVariable: previousBlock.outputvariable };
  };

  const setOutputVariable = (block: RuleBlock, index?: number): RuleBlock => {
    const newBlock = block;
    const blockDict = getDictForFunction(actionsDict, block.function);

    const order = typeof index !== 'undefined' ? (index + 1) : (newActionBlockIndex + 1);

    if ((RULE_BUILDER_TYPES_WITH_OUTPUT as unknown as RuleBuilderTypes).includes(blockDict.return_type)) {
      newBlock.outputvariable = getActionOutputVariableName(order);
    }

    return newBlock;
  };

  const setPrimaryParams = (block: RuleBlock, blocks: RuleBlock[], index?: number): RuleBlock => {
    const blockDict = getDictForFunction(actionsDict, block.function);

    if (!blockDict) return block;

    const newBlock = block;

    Object.keys(block.params).forEach((paramName) => {
      if (getDictForParam(blockDict, paramName)?.primary) {
        if (paramValueExists(block.params[paramName]) && !paramValueIsVariable(block.params[paramName])) {
          return;
        }

        const { previousOutputPresent, outputVariable } = getPreviousOutput(blocks, index);

        if (paramValueExists(block.params[paramName])) {
          if (block.params[paramName] !== `${outputVariable}`) {
            newBlock.params[paramName] = undefined;
          }
        }

        if (!previousOutputPresent) {
          return;
        }

        newBlock.params[paramName] = `$${outputVariable}`;
      }
    });

    return newBlock;
  };

  const setPrimaryParametersAndOutputVariables = (blocks: RuleBlock[]): RuleBlock[] => {
    const blocksWithNewOutput = blocks.map((block, index) => (
      setOutputVariable(block, index)
    ));

    return (
      blocksWithNewOutput.map((block, index) => (
        setPrimaryParams(block, blocksWithNewOutput, index)
      ))
    );
  };

  const addBlock = async (type: BlockType, block: RuleBlock) => {
    let ruleToAdd;
    const blockId = new ObjectID().toString();

    if (type === 'condition') {
      ruleToAdd = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          conditions: [...rule.rule_builder.conditions,
            { ...block, id: blockId },
          ],
        },
      };
    } else {
      const blockToSet = setPrimaryParams(setOutputVariable(block), rule.rule_builder.actions);

      ruleToAdd = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          actions: [...rule.rule_builder.actions,
            { ...blockToSet, id: blockId },
          ],
        },
      };
    }

    validateAndSaveRuleBuilder(ruleToAdd);
  };

  const updateBlock = async (orderIndex: number, type: string, block: RuleBlock) => {
    let ruleToUpdate;

    if (type === 'condition') {
      const currentConditions = [...rule.rule_builder.conditions];

      currentConditions[orderIndex] = block;

      ruleToUpdate = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, conditions: currentConditions,
        },
      };
    } else {
      const currentActions = [...rule.rule_builder.actions];
      const blockToSet = setPrimaryParams(block, currentActions, orderIndex);

      currentActions[orderIndex] = blockToSet;

      ruleToUpdate = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      };
    }

    validateAndSaveRuleBuilder(ruleToUpdate);
  };

  const deleteBlock = async (orderIndex: number, type: BlockType) => {
    let ruleToDelete;

    if (type === 'condition') {
      const currentConditions = [...rule.rule_builder.conditions];
      currentConditions.splice(orderIndex, 1);

      ruleToDelete = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, conditions: currentConditions,
        },
      };
    } else {
      const currentActions = [...rule.rule_builder.actions];
      currentActions.splice(orderIndex, 1);

      ruleToDelete = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: setPrimaryParametersAndOutputVariables(currentActions),
        },
      };
    }

    validateAndSaveRuleBuilder(ruleToDelete);
  };

  const handleCancel = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rules',
      app_action_value: 'cancel-button',
    });

    history.goBack();
  };

  const handleSave = async (event?: React.FormEvent<HTMLFormElement>, closeAfter: boolean = false) => {
    event?.preventDefault();

    if (initialRule) {
      sendTelemetry('click', {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rules',
        app_action_value: closeAfter ? 'update-rule-and-close-button' : 'update-rule-button',
      });

      await updateRule(rule);
      if (closeAfter) handleCancel();
    } else {
      sendTelemetry('click', {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rules',
        app_action_value: 'add-rule-button',
      });

      const result = await createRule(rule);
      if (result?.id) history.replace(`${Routes.SYSTEM.PIPELINES.RULE(result?.id)}?rule_builder=true`);
    }
  };

  const hasRuleBuilderErrors = (): boolean => {
    if (rule.rule_builder.errors?.length > 0) return true;

    if (rule.rule_builder.actions.some(((action) => action.errors?.length > 0))) {
      return true;
    }

    if (rule.rule_builder.conditions.some(((condition) => condition.errors?.length > 0))) {
      return true;
    }

    return false;
  };

  return (
    <form onSubmit={(e) => handleSave(e, true)}>
      <Row className="content">
        <Col xs={8}>
          <RuleBuilderForm rule={rule}
                           onChange={setRule} />
          <label htmlFor="rule_builder">Rule Builder</label>
          <ConvertButton bsStyle="info"
                         bsSize="small"
                         title={initialRule ? 'Convert Rule Builder to Source Code' : 'Please Create the Rule first'}
                         disabled={!initialRule}
                         onClick={() => {
                           sendTelemetry('click', {
                             app_pathname: getPathnameWithoutId(pathname),
                             app_section: 'pipeline-rules',
                             app_action_value: 'convert-rule-builder-to-source-code-button',
                           });

                           setRuleSourceCodeToShow(rule);
                         }}>
            Convert to Source Code
          </ConvertButton>
        </Col>
        <ReferenceRuleCol xs={4}>
          <RuleHelper />
        </ReferenceRuleCol>
        <Col xs={4}>
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
                            order={newConditionBlockIndex}
                            type="condition"
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={() => setBlockToDelete({
                              orderIndex: newConditionBlockIndex,
                              type: 'condition',
                            })} />
        </Col>
        <Col xs={4}>
          <SubTitle htmlFor="rule_builder_actions">Actions</SubTitle>
          {rule.rule_builder.actions.map((action, index) => (
            // eslint-disable-next-line react/no-array-index-key
            <RuleBuilderBlock key={index}
                              blockDict={actionsDict || []}
                              block={action}
                              order={index}
                              type="action"
                              previousOutputPresent={getPreviousOutput(rule.rule_builder.actions, index).previousOutputPresent}
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: index, type: 'action' })} />
          ))}
          <RuleBuilderBlock blockDict={actionsDict || []}
                            order={newActionBlockIndex}
                            type="action"
                            previousOutputPresent={getPreviousOutput(rule.rule_builder.actions, newActionBlockIndex).previousOutputPresent}
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={() => setBlockToDelete({ orderIndex: newActionBlockIndex, type: 'action' })} />
        </Col>
        <Col xs={4}>
          <RuleSimulation rule={rule} />
        </Col>
        <Col xs={12}>
          <Errors objectWithErrors={rule.rule_builder} />
        </Col>
        <ActionsCol xs={12}>
          <FormSubmit disabledSubmit={hasRuleBuilderErrors()}
                      submitButtonText={`${!initialRule ? 'Create rule' : 'Update rule & close'}`}
                      centerCol={initialRule && (
                        <Button type="button" bsStyle="info" onClick={handleSave} disabled={hasRuleBuilderErrors()}>
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
      {ruleSourceCodeToShow && (
        <ConvertToSourceCodeModal show
                                  onHide={() => setRuleSourceCodeToShow(null)}
                                  rule={ruleSourceCodeToShow} />
      )}
    </form>
  );
};

export default RuleBuilder;
