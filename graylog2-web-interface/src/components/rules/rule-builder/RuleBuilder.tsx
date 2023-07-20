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
// eslint-disable-next-line no-irregular-whitespace
import { Toggle } from 'components/navigation/ThemeModeToggle';

import Errors from './Errors';
import RuleBuilderBlock from './RuleBuilderBlock';
import RuleBuilderForm from './RuleBuilderForm';
import { RULE_BUILDER_TYPES_WITH_OUTPUT } from './types';
import type { BlockType, OutputVariables, RuleBlock, RuleBuilderRule, RuleBuilderTypes } from './types';
import {
  getDictForFunction,
  getActionOutputVariableName,
} from './helpers';
import ConvertToSourceCodeModal from './ConvertToSourceCodeModal';

import RuleSimulation from '../RuleSimulation';
import RuleHelper from '../rule-helper/RuleHelper';
import { DEFAULT_SIMULATOR_JSON_MESSAGE, PipelineRulesContext } from '../RuleContext';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const ReferenceRuleCol = styled(Col)`
  .ref-rule {
    height: 100px;
  }

  .input-container {
    display: flex;
  }

  .form-group {
    display: flex;
    flex-direction: column;
  }

  .rule-ref-descriptions {
    display: none;
  }

  .query {
    flex: 1;
  }
`;

const ConvertButton = styled(Button)`
  margin-left: 16px; 
`;

const SubTitle = styled.label`
  color: #aaa;
`;

const SimulatorSwitchContaner = styled.div`
  display: flex;
  align-items: center;
  position: absolute;
  top: 0;
  right: 16px;
  z-index: 1;
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
    simulateRule,
    setRawMessageToSimulate,
  } = useContext(PipelineRulesContext);

  const [rule, setRule] = useState<RuleBuilderRule>({
    description: '',
    title: '',
    rule_builder: { conditions: [], actions: [] },
  });
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const [ruleSourceCodeToShow, setRuleSourceCodeToShow] = useState<RuleBuilderRule | null>(null);
  const [showSimulator, setShowSimulator] = useState<boolean>(false);

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

    if (showSimulator) {
      await simulateRule(DEFAULT_SIMULATOR_JSON_MESSAGE, ruleToAdd);
    }
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

    if (showSimulator) {
      await simulateRule(DEFAULT_SIMULATOR_JSON_MESSAGE, ruleToUpdate);
    }
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

    if (showSimulator) {
      await simulateRule(DEFAULT_SIMULATOR_JSON_MESSAGE, ruleToDelete);
    }
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
        <Col xs={6}>
          <RuleBuilderForm rule={rule}
                           onChange={setRule} />
        </Col>
        <ReferenceRuleCol xs={6}>
          <RuleHelper hideExampleTab />
        </ReferenceRuleCol>
        <Col xs={12}>
          <label htmlFor="rule_builder">Rule Builder</label>
          <ConvertButton bsStyle="info"
                         bsSize="xsmall"
                         title={initialRule ? 'Convert Rule Builder to Source Code' : 'Use Source Code Editor'}
                         onClick={() => {
                           sendTelemetry('click', {
                             app_pathname: getPathnameWithoutId(pathname),
                             app_section: 'pipeline-rules',
                             app_action_value: initialRule ? 'convert-rule-builder-to-source-code-button' : 'source-code-editor-button',
                           });

                           if (initialRule) {
                             setRuleSourceCodeToShow(rule);
                           } else {
                             history.replace(Routes.SYSTEM.PIPELINES.RULE('new'));
                           }
                         }}>
            {initialRule ? 'Convert to Source Code' : 'Source Code Editor'}
          </ConvertButton>
        </Col>
        <Col xs={12}>
          <SimulatorSwitchContaner>
            <Toggle>
              <input type="checkbox"
                     onChange={() => {
                       setShowSimulator(!showSimulator);

                       if (!showSimulator) {
                         setRawMessageToSimulate(DEFAULT_SIMULATOR_JSON_MESSAGE);
                         simulateRule(DEFAULT_SIMULATOR_JSON_MESSAGE, rule);
                       }
                     }}
                     checked={showSimulator} />
              <span className="slider" />
            </Toggle>
            Show Simulator
          </SimulatorSwitchContaner>
          <Row>
            <Col xs={showSimulator ? 4 : 6}>
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
            <Col xs={showSimulator ? 4 : 6}>
              <SubTitle htmlFor="rule_builder_actions">Actions</SubTitle>
              {rule.rule_builder.actions.map((action, index) => (
                // eslint-disable-next-line react/no-array-index-key
                <RuleBuilderBlock key={index}
                                  blockDict={actionsDict || []}
                                  block={action}
                                  order={index}
                                  type="action"
                                  previousOutputPresent={getPreviousOutput(rule.rule_builder.actions, index).previousOutputPresent}
                                  outputVariableList={outputVariableList()}
                                  addBlock={addBlock}
                                  updateBlock={updateBlock}
                                  deleteBlock={() => setBlockToDelete({ orderIndex: index, type: 'action' })} />
              ))}
              <RuleBuilderBlock blockDict={actionsDict || []}
                                order={newActionBlockIndex}
                                type="action"
                                previousOutputPresent={getPreviousOutput(rule.rule_builder.actions, newActionBlockIndex).previousOutputPresent}
                                outputVariableList={outputVariableList()}
                                addBlock={addBlock}
                                updateBlock={updateBlock}
                                deleteBlock={() => setBlockToDelete({ orderIndex: newActionBlockIndex, type: 'action' })} />
            </Col>
            {showSimulator && (
              <Col xs={4}>
                <RuleSimulation rule={rule} />
              </Col>
            )}
          </Row>
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
