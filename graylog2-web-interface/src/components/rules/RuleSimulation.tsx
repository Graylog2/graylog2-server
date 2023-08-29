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

import { DEFAULT_SIMULATOR_JSON_MESSAGE, PipelineRulesContext } from './RuleContext';
import type { RuleBuilderRule } from './rule-builder/types';

const ResetButton = styled(Button)(({ theme }) => css`
  margin-left: ${theme.spacings.xs};
`);

const MessageShowContainer = styled.div(({ theme }) => css`
  padding: ${theme.spacings.md};
`);

const ActionOutputIndex = styled.b`
  color: #aaa;
`;

const StyledFormGroup = styled(FormGroup)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xl};
`);

type Props = {
  rule?: RuleType | RuleBuilderRule,
};

const RuleSimulation = ({ rule: currentRule }: Props) => {
  const {
    rule,
    simulateRule,
    rawMessageToSimulate,
    setRawMessageToSimulate,
    ruleSimulationResult,
    setRuleSimulationResult,
    setStartRuleSimulation,
  } = useContext(PipelineRulesContext);

  const actionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_action_variables || {}).sort();
  const conditionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_condition_variables || {}).sort();

  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => () => {
    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
  }, [setRawMessageToSimulate, setRuleSimulationResult, setStartRuleSimulation]);

  const is_rule_builder = Boolean(currentRule?.rule_builder);

  const handleRawMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawMessageToSimulate(event.target.value);
  };

  const handleRunRuleSimulation = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'run-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    simulateRule(
      rawMessageToSimulate,
      currentRule || rule,
      setRuleSimulationResult,
    );
  };

  const handleResetRuleSimulation = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'reset-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    setRawMessageToSimulate('');
    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
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
               help="Enter a normal string to simulate the message field or a JSON to simulate the whole message."
               rows={3} />
        <Button bsStyle="info"
                bsSize="xsmall"
                disabled={!rawMessageToSimulate}
                onClick={handleRunRuleSimulation}>
          Run rule simulation
        </Button>
        <ResetButton bsStyle="default"
                     bsSize="xsmall"
                     onClick={handleResetRuleSimulation}>
          Reset
        </ResetButton>
        {ruleSimulationResult && (
          <>
            <MessageShowContainer>
              <MessageShow message={ruleSimulationResult} />
            </MessageShowContainer>
            {is_rule_builder && (
              <>
                {conditionsOutputKeys.length > 0 && (
                  <div data-testid="conditions-output">
                    <label htmlFor="simulation_conditions_output">Conditions Output</label>
                    {conditionsOutputKeys.map((conditionsOutputKey) => (
                      <div key={conditionsOutputKey}>
                        <ActionOutputIndex>{conditionsOutputKey}</ActionOutputIndex>: {String(ruleSimulationResult?.simulator_condition_variables[conditionsOutputKey])}
                      </div>
                    ))}
                  </div>
                )}
                <br />
                {actionsOutputKeys.length > 0 && (
                  <div data-testid="actions-output">
                    <label htmlFor="simulation_actions_output">Actions Output</label>
                    {actionsOutputKeys.map((actionsOutputKey) => (
                      <div key={actionsOutputKey}>
                        <ActionOutputIndex>{actionsOutputKey}</ActionOutputIndex>: {String(ruleSimulationResult?.simulator_action_variables[actionsOutputKey])}
                      </div>
                    ))}
                  </div>
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
};

RuleSimulation.defaultProps = {
  rule: undefined,
};

export default RuleSimulation;
