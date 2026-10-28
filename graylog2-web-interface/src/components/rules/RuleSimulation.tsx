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
import React, { useContext, useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Button, ControlLabel, FormGroup, Input, ButtonGroup } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import type { RuleType } from 'components/rules/hooks/useRules';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import * as JSON from 'util/json';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { PipelineRulesContext, SimulationFieldType } from './RuleContext';
import type { RuleBuilderRule } from './rule-builder/types';
import { useRuleBuilder } from './rule-builder/RuleBuilderContext';
import { hasRuleBuilderErrors } from './rule-builder/helpers';

const ResetButton = styled(Button)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xs};
  `,
);

const MessageShowContainer = styled.div(
  ({ theme }) => css`
    padding: ${theme.spacings.sm};
  `,
);

const ActionOutputIndex = styled.span`
  color: #aaa;
`;

const OutputContainer = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};
  `,
);

const OutputText = styled.div<{ $highlighted?: boolean }>(
  ({ $highlighted, theme }) => css`
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    color: ${$highlighted ? theme.colors.variant.info : 'inherit'};
    font-weight: ${$highlighted ? 'bold' : 'inherit'};
  `,
);

const StyledFormGroup = styled(FormGroup)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xl};
  `,
);

type Props = {
  rule?: RuleType | RuleBuilderRule;
  onSaveMessage?: (message: string) => void;
};

const RuleSimulation = ({ rule: currentRule = undefined, onSaveMessage = () => {} }: Props) => {
  const { t } = useTranslation('pipelines');
  const {
    rule,
    simulateRule,
    rawMessageToSimulate,
    setRawMessageToSimulate,
    ruleSimulationResult,
    setRuleSimulationResult,
  } = useContext(PipelineRulesContext);

  const [highlightedOutput] = useRuleBuilder().useHighlightedOutput;
  const sendTelemetry = useSendTelemetry('pipeline-rule-simulation');
  const [simulationFieldType, setSimulationFieldType] = useState(SimulationFieldType.JSON);
  const [simulationErrorMessage, setSimulationErrorMessage] = useState(undefined);

  useEffect(
    () => () => {
      setRuleSimulationResult(null);
    },
    [setRuleSimulationResult],
  );

  useEffect(() => {
    if (hasRuleBuilderErrors(currentRule)) {
      setRuleSimulationResult(null);
    } else if (currentRule) {
      simulateRule(currentRule, simulationFieldType);
    }
  }, [currentRule, setRuleSimulationResult, simulateRule, simulationFieldType]);

  const is_rule_builder = Boolean(currentRule?.rule_builder);
  const ruleErrorMessage = hasRuleBuilderErrors(currentRule)
    ? t('rule_simulation.error_builder')
    : undefined;
  const conditionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_condition_variables || {}).sort(
    (a, b) => Number(a) - Number(b),
  );

  const getPlaceHolderByType = () => {
    switch (simulationFieldType) {
      case SimulationFieldType.Simple:
        return t('rule_simulation.placeholder_simple');
      case SimulationFieldType.KeyValue:
        return 'message: test\nsource: unknown\n';
      case SimulationFieldType.JSON:
        return '{\n\tmessage: test\n\tsource: unknown\n}';
      default:
        return t('rule_simulation.placeholder_simple');
    }
  };

  const validateFieldType = () => {
    setSimulationErrorMessage(undefined);

    if (simulationFieldType === SimulationFieldType.JSON) {
      try {
        JSON.parse(rawMessageToSimulate);
      } catch {
        setSimulationErrorMessage(t('rule_simulation.invalid_json'));
      }
    }
  };

  const handleFieldTypeChange = (fieldType: SimulationFieldType) => {
    setSimulationFieldType(fieldType);
    setSimulationErrorMessage(undefined);
  };

  const handleRawMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawMessageToSimulate(event.target.value);
    onSaveMessage(event.target.value);
  };

  const handleRunRuleSimulation = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.RUN_RULE_SIMULATION_CLICKED, {
      app_action_value: 'run-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    validateFieldType();
    simulateRule(currentRule || rule, simulationFieldType);
  };

  const handleResetRuleSimulation = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.RESET_RULE_SIMULATION_CLICKED, {
      app_action_value: 'reset-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    setSimulationErrorMessage(undefined);
    setRawMessageToSimulate('');
    onSaveMessage(null);
    setRuleSimulationResult(null);
  };

  return (
    <StyledFormGroup>
      <ControlLabel>
        {t('rule_simulation.title')} <small className="text-muted">{t('rule_simulation.optional')}</small>
      </ControlLabel>
      <div>
        <ButtonGroup>
          <Button
            active={simulationFieldType === SimulationFieldType.JSON}
            onClick={() => handleFieldTypeChange(SimulationFieldType.JSON)}>
            {t('rule_simulation.json')}
          </Button>
          <Button
            active={simulationFieldType === SimulationFieldType.KeyValue}
            onClick={() => handleFieldTypeChange(SimulationFieldType.KeyValue)}>
            {t('rule_simulation.key_value')}
          </Button>
          <Button
            active={simulationFieldType === SimulationFieldType.Simple}
            onClick={() => handleFieldTypeChange(SimulationFieldType.Simple)}>
            {t('rule_simulation.simple_message')}
          </Button>
        </ButtonGroup>
        <Input
          id="message"
          type="textarea"
          placeholder={getPlaceHolderByType()}
          value={rawMessageToSimulate}
          onChange={handleRawMessageChange}
          title={t('rule_simulation.input_title')}
          help={t('rule_simulation.input_help')}
          error={ruleErrorMessage || simulationErrorMessage}
          rows={4}
        />
        <Button
          bsStyle="info"
          bsSize="xsmall"
          disabled={!rawMessageToSimulate || Boolean(ruleErrorMessage)}
          onClick={handleRunRuleSimulation}>
          {t('rule_simulation.run')}
        </Button>
        <ResetButton bsStyle="default" bsSize="xsmall" onClick={handleResetRuleSimulation}>
          {t('rule_simulation.reset')}
        </ResetButton>
        {rawMessageToSimulate && ruleSimulationResult && (
          <>
            <MessageShowContainer>
              <MessageShow message={ruleSimulationResult} />
            </MessageShowContainer>
            {is_rule_builder && (
              <>
                {conditionsOutputKeys.length > 0 && (
                  <OutputContainer data-testid="conditions-output">
                    <label htmlFor="simulation_conditions_output">{t('rule_simulation.conditions_output')}</label>
                    {conditionsOutputKeys.map((conditionsOutputKey) => (
                      <OutputText key={conditionsOutputKey}>
                        <ActionOutputIndex>{conditionsOutputKey}</ActionOutputIndex>:{' '}
                        {JSON.stringify(ruleSimulationResult?.simulator_condition_variables[conditionsOutputKey])}
                      </OutputText>
                    ))}
                  </OutputContainer>
                )}
                {ruleSimulationResult?.simulator_action_variables?.length > 0 && (
                  <OutputContainer data-testid="actions-output">
                    <label htmlFor="simulation_actions_output">{t('rule_simulation.actions_output')}</label>
                    {ruleSimulationResult?.simulator_action_variables?.map(
                      (actionOutputKeyValue: Record<string, unknown>) => {
                        const keyValue = Object.entries(actionOutputKeyValue)[0];

                        return (
                          <OutputText
                            key={keyValue[0]}
                            $highlighted={highlightedOutput === keyValue[0]}
                            title={JSON.stringify(keyValue[1])}>
                            <ActionOutputIndex>${keyValue[0]}</ActionOutputIndex>: {JSON.stringify(keyValue[1])}
                          </OutputText>
                        );
                      },
                    )}
                  </OutputContainer>
                )}
              </>
            )}
          </>
        )}
      </div>
    </StyledFormGroup>
  );
};

export default RuleSimulation;
