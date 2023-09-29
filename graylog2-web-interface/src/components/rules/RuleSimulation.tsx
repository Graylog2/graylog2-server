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
import React, { useContext, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';

import { Button, ControlLabel, FormGroup, Input } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import type { RuleType } from 'stores/rules/RulesStore';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { DEFAULT_SIMULATOR_JSON_MESSAGE, PipelineRulesContext } from './RuleContext';
import type { RuleBuilderRule } from './rule-builder/types';
import { useRuleBuilder } from './rule-builder/RuleBuilderContext';
import { hasRuleBuilderErrors } from './rule-builder/helpers';

const ResetButton = styled(Button)(({ theme }) => css`
  margin-left: ${theme.spacings.xs};
`);

const MessageShowContainer = styled.div(({ theme }) => css`
  padding: ${theme.spacings.sm};
`);

const ActionOutputIndex = styled.span`
  color: #aaa;
`;

const OutputContainer = styled.div(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const OutputText = styled.div<{ $highlighted?: boolean }>(({ $highlighted, theme }) => css`
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  color: ${$highlighted ? theme.colors.variant.info : 'inherit'};
  font-weight: ${$highlighted ? 'bold' : 'inherit'};
`);

const StyledFormGroup = styled(FormGroup)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xl};
`);

type Props = {
  rule?: RuleType | RuleBuilderRule,
  onSaveMessage?: (message: string) => void,
};

const RuleSimulation = ({ rule: currentRule, onSaveMessage }: Props) => {
  const {
    rule,
    simulateRule,
    rawMessageToSimulate,
    setRawMessageToSimulate,
    ruleSimulationResult,
    setRuleSimulationResult,
  } = useContext(PipelineRulesContext);

  const [highlightedOutput] = useRuleBuilder().useHighlightedOutput;
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => () => {
    setRuleSimulationResult(null);
  }, [setRuleSimulationResult]);

  useEffect(() => {
    if (hasRuleBuilderErrors(currentRule)) {
      setRuleSimulationResult(null);
    } else if (currentRule) {
      simulateRule(currentRule);
    }
  }, [currentRule, setRuleSimulationResult, simulateRule]);

  const is_rule_builder = Boolean(currentRule?.rule_builder);
  const errorMessage = hasRuleBuilderErrors(currentRule) ? 'Could not run the rule simulation. Please fix the rule builder errors.' : undefined;
  const conditionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_condition_variables || {}).sort((a, b) => Number(a) - Number(b));

  const handleRawMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawMessageToSimulate(event.target.value);
    onSaveMessage(event.target.value);
  };

  const handleRunRuleSimulation = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.RUN_RULE_SIMULATION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'run-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    simulateRule(currentRule || rule);
  };

  const handleResetRuleSimulation = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.RESET_RULE_SIMULATION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'reset-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    setRawMessageToSimulate('');
    onSaveMessage(null);
    setRuleSimulationResult(null);
  };

  return (
    <StyledFormGroup>
      <ControlLabel>Rule Simulation <small className="text-muted">(Optional)</small></ControlLabel>
      <div>
        <Input id="message"
               type="textarea"
               placeholder={DEFAULT_SIMULATOR_JSON_MESSAGE}
               value={rawMessageToSimulate}
               onChange={handleRawMessageChange}
               title="Message string or JSON"
               help="Enter a normal string to simulate the message field, Key-Value pairs or a JSON to simulate the whole message."
               error={errorMessage}
               rows={3} />
        <Button bsStyle="info"
                bsSize="xsmall"
                disabled={!rawMessageToSimulate || Boolean(errorMessage)}
                onClick={handleRunRuleSimulation}>
          Run rule simulation
        </Button>
        <ResetButton bsStyle="default"
                     bsSize="xsmall"
                     onClick={handleResetRuleSimulation}>
          Reset
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
                    <label htmlFor="simulation_conditions_output">Conditions Output</label>
                    {conditionsOutputKeys.map((conditionsOutputKey) => (
                      <OutputText key={conditionsOutputKey}>
                        <ActionOutputIndex>{conditionsOutputKey}</ActionOutputIndex>: {JSON.stringify(ruleSimulationResult?.simulator_condition_variables[conditionsOutputKey])}
                      </OutputText>
                    ))}
                  </OutputContainer>
                )}
                {ruleSimulationResult?.simulator_action_variables?.length > 0 && (
                  <OutputContainer data-testid="actions-output">
                    <label htmlFor="simulation_actions_output">Actions Output</label>
                    {ruleSimulationResult?.simulator_action_variables?.map((actionOutputKeyValue) => {
                      const keyValue = Object.entries(actionOutputKeyValue)[0];

                      return (
                        <OutputText key={keyValue[0]}
                                    $highlighted={highlightedOutput === keyValue[0]}
                                    title={JSON.stringify(keyValue[1])}>
                          <ActionOutputIndex>${keyValue[0]}</ActionOutputIndex>: {JSON.stringify(keyValue[1])}
                        </OutputText>
                      );
                    })}
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

RuleSimulation.propTypes = {
  rule: PropTypes.object,
  onSaveMessage: PropTypes.func,
};

RuleSimulation.defaultProps = {
  rule: undefined,
  onSaveMessage: () => {},
};

export default RuleSimulation;
