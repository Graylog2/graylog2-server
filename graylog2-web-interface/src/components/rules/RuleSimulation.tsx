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

import { Button, ControlLabel, FormGroup, Input } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import type { RuleType } from 'stores/rules/RulesStore';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';

import { PipelineRulesContext } from './RuleContext';
import type { RuleBuilderRule } from './rule-builder/types';

const RuleSimulationFormGroup = styled(FormGroup)`
  margin-bottom: 40px;
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

const ActionsOutputContainer = styled.div`
  margin-top: 16px;
`;

type Props = {
  rule?: RuleType | RuleBuilderRule,
};

const RuleSimulation = ({ rule: currentRule }: Props) => {
  const {
    rule,
    ruleSource,
    simulateRule,
    rawMessageToSimulate,
    setRawMessageToSimulate,
    ruleSimulationResult,
    setRuleSimulationResult,
    setStartRuleSimulation,
  } = useContext(PipelineRulesContext);

  const actionsOuputPrefix = 'gl2_simulator_output_actions';
  const actionsOutputKeys = Object.keys(ruleSimulationResult?.fields || {}).filter((key) => key.includes(actionsOuputPrefix));
  const getActionOutpuKey = (actionIndex: number) => `${actionsOuputPrefix}_${actionIndex}`;

  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => () => {
    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
  }, [setRawMessageToSimulate, setRuleSimulationResult, setStartRuleSimulation]);

  const disableSimulation = !rawMessageToSimulate || (!ruleSource && !currentRule?.rule_builder?.conditions?.length && !currentRule?.rule_builder?.actions?.length);
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

    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
  };

  return (
    <RuleSimulationFormGroup>
      <ControlLabel>Rule Simulation <small className="text-muted">(Optional)</small></ControlLabel>
      <div>
        <Input id="message"
               type="textarea"
               // eslint-disable-next-line quotes
               placeholder={`{\n    "message": "test"\n}`}
               value={rawMessageToSimulate}
               onChange={handleRawMessageChange}
               title="Message string or JSON"
               help="Enter a normal string to simulate the message field or a JSON to simulate the whole message."
               rows={3} />
        <Button bsStyle="info"
                bsSize="xsmall"
                disabled={disableSimulation}
                onClick={handleRunRuleSimulation}>
          Run rule simulation
        </Button>
        <ResetButton bsStyle="default"
                     bsSize="xsmall"
                     onClick={handleResetRuleSimulation}>
          Reset
        </ResetButton>
        {ruleSimulationResult && (
          <ActionsOutputContainer>
            <label htmlFor="simulation_actions_output">Actions Output</label>
            {actionsOutputKeys.map((actionsOutputKey, key) => (
              <div key={actionsOutputKey}>
                <ActionOutputIndex>Action {key + 1}</ActionOutputIndex>: {ruleSimulationResult?.fields[getActionOutpuKey(key + 1)]}
              </div>
            ))}
          </ActionsOutputContainer>
        )}
        {ruleSimulationResult && (
          <MessageShowContainer>
            <MessageShow message={ruleSimulationResult} />
          </MessageShowContainer>
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
