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
import styled, { css } from 'styled-components';
import ObjectID from 'bson-objectid';

import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { Row, Col, Button, Panel, Radio } from 'components/bootstrap';
import useRuleBuilder from 'hooks/useRuleBuilder';
import { ConfirmDialog, FormSubmit } from 'components/common';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import RuleBuilderProvider from './RuleBuilderProvider';
import RuleBuilderBlock from './RuleBuilderBlock';
import RuleBuilderForm from './RuleBuilderForm';
import type { BlockType, OutputVariables, RuleBlock, RuleBuilderRule } from './types';
import { RuleBuilderTypes } from './types';
import { getDictForFunction, hasRuleBuilderErrors } from './helpers';
import ConvertToSourceCodeModal from './ConvertToSourceCodeModal';
import Errors from './Errors';

import RuleSimulation from '../RuleSimulation';

const ActionsCol = styled(Col)`
  margin-top: 50px;
`;

const StyledPanel = styled(Panel)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 0;
  box-shadow: none;
  margin-bottom: 0;
`);

const StyledPanelHeading = styled(Panel.Heading)(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  background-color: ${theme.colors.table.backgroundAlt} !important;
  border: 0;
`);

const WhenOperator = styled.div`
  display: flex;

  .radio {
    margin: 0 8px;
  }
`;

const StyledPanelBody = styled(Panel.Body)`
  border: 0;
  padding: 0;
`;

const getLastOutputIndexFromRule = (rule: RuleBuilderRule): number => {
  const outputIndexes = rule.rule_builder?.actions?.map((block: RuleBlock) => Number(block?.outputvariable?.replace('output_', '') || 0))?.sort((a, b) => a - b) || [];

  return outputIndexes[outputIndexes.length - 1] || 0;
};

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
    rule_builder: { operator: 'AND', conditions: [], actions: [] },
  });
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const [ruleSourceCodeToShow, setRuleSourceCodeToShow] = useState<RuleBuilderRule | null>(null);
  const [conditionsExpanded] = useState<boolean>(true);
  const [actionsExpanded] = useState<boolean>(true);
  const [lastOutputIndex, setLastOutputIndex] = useState<number>(0);

  useEffect(() => {
    if (initialRule) {
      setRule(initialRule);
      setLastOutputIndex(getLastOutputIndexFromRule(initialRule));
    }
  }, [initialRule]);

  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const setOutputVariable = (block: RuleBlock, outputIndex: number): RuleBlock => {
    const newBlock = block;
    const blockDict = getDictForFunction(actionsDict, block.function);

    if (
      blockDict?.return_type !== RuleBuilderTypes.Void
      && !newBlock.outputvariable
    ) {
      newBlock.outputvariable = `output_${outputIndex}`;
      setLastOutputIndex(outputIndex);
    }

    return newBlock;
  };

  const newConditionBlockIndex = rule.rule_builder.conditions.length;
  const newActionBlockIndex = rule.rule_builder.actions.length;

  const validateAndSaveRuleBuilder = (ruleToValidate: RuleBuilderRule) => fetchValidateRule(ruleToValidate).then((ruleValidated) => {
    setRule({ ...ruleToValidate, rule_builder: ruleValidated.rule_builder, source: ruleValidated.source });
  }).catch(() => setRule(ruleToValidate));

  const saveSimulatorMessage = async (simulator_message: string) => {
    const newOperatorRule: RuleBuilderRule = {
      ...rule,
      simulator_message,
    };

    setRule(newOperatorRule);
  };

  const updateWhenOperator = async (operator: 'AND'|'OR') => {
    sendTelemetry(
      operator === 'AND'
        ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.OPERATOR_AND_CLICKED
        : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.OPERATOR_OR_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rules',
        app_action_value: 'cancel-button',
      });

    const newOperatorRule: RuleBuilderRule = {
      ...rule,
      rule_builder: {
        ...rule.rule_builder,
        operator,
      },
    };

    await validateAndSaveRuleBuilder(newOperatorRule);
  };

  const addBlock = async (type: BlockType, block: RuleBlock, orderIndex?: number) => {
    let ruleToAdd: RuleBuilderRule;
    const blockId = new ObjectID().toString();

    if (type === 'condition') {
      const newConditions = rule.rule_builder.conditions;
      newConditions.splice(orderIndex || newConditions.length, 0, { ...block, id: blockId });

      ruleToAdd = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          conditions: newConditions,
        },
      };
    } else {
      const blockToSet = setOutputVariable(block, lastOutputIndex + 1);
      const newActions = rule.rule_builder.actions;
      newActions.splice(Number.isInteger(orderIndex) ? orderIndex : newActions.length, 0, { ...blockToSet, id: blockId });

      ruleToAdd = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder,
          actions: newActions,
        },
      };
    }

    await validateAndSaveRuleBuilder(ruleToAdd);
  };

  const updateBlock = async (orderIndex: number, type: string, block: RuleBlock) => {
    let ruleToUpdate: RuleBuilderRule;

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

      currentActions[orderIndex] = setOutputVariable(block, lastOutputIndex + 1);

      ruleToUpdate = {
        ...rule,
        rule_builder: {
          ...rule.rule_builder, actions: currentActions,
        },
      };
    }

    await validateAndSaveRuleBuilder(ruleToUpdate);
  };

  const deleteBlock = async (orderIndex: number, type: BlockType) => {
    let ruleToDelete: RuleBuilderRule;

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
          ...rule.rule_builder, actions: currentActions,
        },
      };
    }

    await validateAndSaveRuleBuilder(ruleToDelete);
  };

  const handleCancel = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.CANCEL_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rules',
      app_action_value: 'cancel-button',
    });

    history.push(Routes.SYSTEM.PIPELINES.RULES);
  };

  const handleSave = async (event?: React.SyntheticEvent<HTMLElement>, closeAfter: boolean = false) => {
    event?.preventDefault();

    if (initialRule) {
      sendTelemetry(
        closeAfter
          ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.UPDATE_RULE_AND_CLOSE_CLICKED
          : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.UPDATE_RULE_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'pipeline-rules',
          app_action_value: closeAfter ? 'update-rule-and-close-button' : 'update-rule-button',
        });

      await updateRule(rule);
      if (closeAfter) handleCancel();
    } else {
      sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.ADD_RULE_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rules',
        app_action_value: 'add-rule-button',
      });

      const result = await createRule(rule);
      if (result?.id) history.replace(`${Routes.SYSTEM.PIPELINES.RULE(result?.id)}?rule_builder=true`);
    }
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
    <RuleBuilderProvider>
      <form onSubmit={(e) => handleSave(e, true)}>
        <Row className="content">
          <Col xs={12}>
            <RuleBuilderForm rule={rule}
                             onChange={setRule} />
          </Col>
          <Col xs={8}>
            <label htmlFor="rule_builder">Rule Builder</label>
            <StyledPanel expanded={conditionsExpanded}>
              <StyledPanelHeading>
                <Panel.Title toggle>
                  When
                </Panel.Title>
                <WhenOperator>
                  <Radio checked={rule.rule_builder.operator === 'AND'}
                         onChange={() => updateWhenOperator('AND')}>
                    and
                  </Radio>
                  <Radio checked={rule.rule_builder.operator === 'OR'}
                         onChange={() => updateWhenOperator('OR')}>
                    or
                  </Radio>
                </WhenOperator>
              </StyledPanelHeading>
              <Panel.Collapse>
                <StyledPanelBody>
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
                </StyledPanelBody>
              </Panel.Collapse>
            </StyledPanel>
            <br />
            <StyledPanel expanded={actionsExpanded}>
              <StyledPanelHeading>
                <Panel.Title toggle>
                  Then
                </Panel.Title>
              </StyledPanelHeading>
              <Panel.Collapse>
                <StyledPanelBody>
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
                </StyledPanelBody>
              </Panel.Collapse>
            </StyledPanel>
            <Errors objectWithErrors={rule.rule_builder} />
          </Col>
          <Col xs={4}>
            <RuleSimulation rule={rule} onSaveMessage={saveSimulatorMessage} />
          </Col>
          <ActionsCol xs={12}>
            <FormSubmit disabledSubmit={hasRuleBuilderErrors(rule)}
                        submitButtonText={!initialRule ? 'Create rule' : 'Update rule & close'}
                        centerCol={initialRule && (
                        <>
                          <Button type="button" bsStyle="info" onClick={handleSave} disabled={hasRuleBuilderErrors(rule)}>
                            Update rule
                          </Button>
                          <Button bsStyle="info"
                                  title="Convert Rule Builder to Source Code"
                                  disabled={hasRuleBuilderErrors(rule)}
                                  onClick={() => {
                                    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.CONVERT_TO_SOURCE_CODE_CLICKED, {
                                      app_pathname: getPathnameWithoutId(pathname),
                                      app_section: 'pipeline-rules',
                                      app_action_value: 'convert-rule-builder-to-source-code-button',
                                    });

                                    setRuleSourceCodeToShow(rule);
                                  }}>
                            Convert to Source Code
                          </Button>
                        </>
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
                                  onNavigateAway={updateRule}
                                  onHide={() => setRuleSourceCodeToShow(null)}
                                  rule={ruleSourceCodeToShow} />
        )}
      </form>
    </RuleBuilderProvider>
  );
};

export default RuleBuilder;
