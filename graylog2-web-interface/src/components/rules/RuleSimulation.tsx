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
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { Button, FormGroup, Input } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import type { RuleType } from 'stores/rules/RulesStore';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';

import { DEFAULT_SIMULATOR_JSON_MESSAGE, PipelineRulesContext } from './RuleContext';
import type { RuleBuilderRule } from './rule-builder/types';

const RuleSimulationFormGroup = styled(FormGroup)`
  margin-bottom: 40px;
  margin-top: 25px;
`;

const ResetButton = styled(Button)`
  margin-left: 8px;
`;

const MessageShowContainer = styled.div`
  padding: 16px;
`;

const ActionOutputIndex = styled.b`
  color: #aaa;
`;

const OutputContainer = styled.div`
  margin-top: 8px;
`;

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

  const actionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_action_variables || {});
  const getActionOutpuKey = (actionIndex: number) => `actions_${actionIndex}`;

  const conditionsOutputKeys = Object.keys(ruleSimulationResult?.simulator_condition_variables || {});
  const getConditionOutpuKey = (conditionIndex: number) => `${conditionIndex}`;

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
    <RuleSimulationFormGroup>
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
            <OutputContainer>
              <label htmlFor="simulation_conditions_output">Conditions Output</label>
              {conditionsOutputKeys.map((conditionsOutputKey, key) => (
                <div key={conditionsOutputKey}>
                  <ActionOutputIndex>Condition {key + 1}</ActionOutputIndex>: {String(ruleSimulationResult?.simulator_condition_variables[getConditionOutpuKey(key + 1)])}
                </div>
              ))}
            </OutputContainer>
            <OutputContainer>
              <label htmlFor="simulation_actions_output">Actions Output</label>
              {actionsOutputKeys.map((actionsOutputKey, key) => (
                <div key={actionsOutputKey}>
                  <ActionOutputIndex>Action {key + 1}</ActionOutputIndex>: {ruleSimulationResult?.simulator_action_variables[getActionOutpuKey(key + 1)]}
                </div>
              ))}
            </OutputContainer>
          </>
        )}
      </div>
    </RuleSimulationFormGroup>
  );
};

RuleSimulation.propTypes = {
  rule: PropTypes.object,
};

RuleSimulation.defaultProps = {
  rule: undefined,
};

export default RuleSimulation;
