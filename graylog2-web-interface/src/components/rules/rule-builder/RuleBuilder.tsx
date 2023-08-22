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
import React, { useState, useEffect, useContext } from 'react';
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
import type { BlockType, OutputVariables, RuleBlock, RuleBuilderRule, RuleBuilderTypes } from './types';
import {
  getDictForFunction,
} from './helpers';
import ConvertToSourceCodeModal from './ConvertToSourceCodeModal';
import ConfirmNavigateToSourceCodeEditorModal from './ConfirmNavigateToSourceCodeEditorModal';

import RuleSimulation from '../RuleSimulation';
import { PipelineRulesContext } from '../RuleContext';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const RuleBuilderCol = styled(Col)`
  display: flex;
  justify-content: space-between;
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

  const {
    rawMessageToSimulate,
    simulateRule,
  } = useContext(PipelineRulesContext);

  const [rule, setRule] = useState<RuleBuilderRule>({
    description: '',
    title: '',
    rule_builder: { conditions: [], actions: [] },
  });
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const [ruleSourceCodeToShow, setRuleSourceCodeToShow] = useState<RuleBuilderRule | null>(null);
  const [showConfirmSourceCodeEditor, setShowConfirmSourceCodeEditor] = useState<boolean>(false);

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

  const isFormDirty = (_rule: RuleBuilderRule) => Boolean(rule.title || _rule.rule_builder.conditions.length || _rule.rule_builder.conditions.length);

  const getActionOutputVariableName = (order : number) : string => {
    if (order === 0) return '';

    return `output_${order}`;
  };

  const validateAndSaveRuleBuilder = (ruleToValidate: RuleBuilderRule) => fetchValidateRule(ruleToValidate).then((ruleValidated) => {
    setRule({ ...ruleToValidate, rule_builder: ruleValidated.rule_builder });
  }).catch(() => setRule(ruleToValidate));

  const setOutputVariable = (block: RuleBlock, index?: number): RuleBlock => {
    const newBlock = block;
    const blockDict = getDictForFunction(actionsDict, block.function);

    const order = typeof index !== 'undefined' ? (index + 1) : (newActionBlockIndex + 1);

    if ((RULE_BUILDER_TYPES_WITH_OUTPUT as unknown as RuleBuilderTypes).includes(blockDict?.return_type)) {
      newBlock.outputvariable = getActionOutputVariableName(order);
    }

    return newBlock;
  };

  const setOutputVariables = (blocks: RuleBlock[]): RuleBlock[] => (
    blocks.map((block, index) => (
      setOutputVariable(block, index)
    ))
  );

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
      const blockToSet = setOutputVariable(block);

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

    await validateAndSaveRuleBuilder(ruleToAdd);
    await simulateRule(rawMessageToSimulate, ruleToAdd);
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

      currentActions[orderIndex] = block;

      ruleToUpdate = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      };
    }

    await validateAndSaveRuleBuilder(ruleToUpdate);
    await simulateRule(rawMessageToSimulate, ruleToUpdate);
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
          ...rule.rule_builder, actions: setOutputVariables(currentActions),
        },
      };
    }

    await validateAndSaveRuleBuilder(ruleToDelete);
    await simulateRule(rawMessageToSimulate, ruleToDelete);
  };

  const handleCancel = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rules',
      app_action_value: 'cancel-button',
    });

    history.replace(Routes.SYSTEM.PIPELINES.RULES);
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

  const outputVariableList = () : OutputVariables => (
    rule.rule_builder.actions.map((block: RuleBlock, index) => ({
      variableName: block.outputvariable ? `$${block.outputvariable}` : null,
      variableType: getDictForFunction(actionsDict, block.function)?.return_type,
      stepOrder: index,
      blockId: block.id,
    })).filter(({ variableName }) => (variableName !== null))
  );

  return (
    <form onSubmit={(e) => handleSave(e, true)}>
      <Row className="content">
        <Col xs={12}>
          <RuleBuilderForm rule={rule}
                           onChange={setRule} />
        </Col>
        <RuleBuilderCol xs={4}>
          <label htmlFor="rule_builder">Rule Builder</label>
          <Button bsStyle="info"
                  bsSize="small"
                  title={initialRule ? 'Convert Rule Builder to Source Code' : 'Use Source Code Editor'}
                  onClick={() => {
                    sendTelemetry('click', {
                      app_pathname: getPathnameWithoutId(pathname),
                      app_section: 'pipeline-rules',
                      app_action_value: initialRule ? 'convert-rule-builder-to-source-code-button' : 'source-code-editor-button',
                    });

                    if (initialRule) {
                      setRuleSourceCodeToShow(rule);
                    } else if (isFormDirty(rule)) {
                      setShowConfirmSourceCodeEditor(true);
                    } else {
                      history.push(Routes.SYSTEM.PIPELINES.RULE('new'));
                    }
                  }}>
            {initialRule ? 'Convert to Source Code' : 'Source Code Editor'}
          </Button>
        </RuleBuilderCol>
        <Col xs={12}>
          <Row>
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
                                  outputVariableList={outputVariableList()}
                                  addBlock={addBlock}
                                  updateBlock={updateBlock}
                                  deleteBlock={() => setBlockToDelete({ orderIndex: index, type: 'action' })} />
              ))}
              <RuleBuilderBlock blockDict={actionsDict || []}
                                order={newActionBlockIndex}
                                type="action"
                                outputVariableList={outputVariableList()}
                                addBlock={addBlock}
                                updateBlock={updateBlock}
                                deleteBlock={() => setBlockToDelete({ orderIndex: newActionBlockIndex, type: 'action' })} />
            </Col>
            <Col xs={4}>
              <RuleSimulation rule={rule} />
            </Col>
          </Row>
        </Col>
        <Col xs={12}>
          <Errors objectWithErrors={rule.rule_builder} />
        </Col>
        <ActionsCol xs={12}>
          <FormSubmit disabledSubmit={hasRuleBuilderErrors()}
                      submitButtonText={!initialRule ? 'Create rule' : 'Update rule & close'}
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
      {showConfirmSourceCodeEditor && (
        <ConfirmNavigateToSourceCodeEditorModal show
                                                rule={rule}
                                                onHide={() => setShowConfirmSourceCodeEditor(false)}
                                                onSave={createRule} />
      )}
    </form>
  );
};

export default RuleBuilder;
