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
import React, { useContext } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { Button, ControlLabel, FormGroup, Input } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import type { RuleType } from 'stores/rules/RulesStore';
import { getBasePathname } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

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

type Props = {
  rule?: RuleType|RuleBuilderRule,
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
    startRuleSimulation,
    setStartRuleSimulation,
  } = useContext(PipelineRulesContext);

  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const disableSimulation = !rawMessageToSimulate || (!ruleSource && !currentRule?.rule_builder?.conditions?.length && !currentRule?.rule_builder?.actions?.length);
  const is_rule_builder = Boolean(currentRule?.rule_builder);

  const handleRawMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawMessageToSimulate(event.target.value);
  };

  const handleRunRuleSimulation = () => {
    sendTelemetry('click', {
      app_pathname: getBasePathname(pathname),
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
      app_pathname: getBasePathname(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'reset-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    setRawMessageToSimulate('');
    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
  };

  const handleStartRuleSimulation = () => {
    sendTelemetry('click', {
      app_pathname: getBasePathname(pathname),
      app_section: 'pipeline-rule-simulation',
      app_action_value: 'start-rule-simulation-button',
      event_details: { is_rule_builder },
    });

    setStartRuleSimulation(true);
  };

  return (
    <RuleSimulationFormGroup>
      <ControlLabel>Rule Simulation <small className="text-muted">(Optional)</small></ControlLabel>
      <div>
        {!startRuleSimulation && (
        <Button bsStyle="info"
                bsSize="xsmall"
                onClick={handleStartRuleSimulation}>
          Start rule simulation
        </Button>
        )}
        {startRuleSimulation && (
        <>
          <Input id="message"
                 type="textarea"
                 // eslint-disable-next-line quotes
                 placeholder={`{\n    "message": "test"\n}`}
                 value={rawMessageToSimulate}
                 onChange={handleRawMessageChange}
                 title="Message string or JSON"
                 help="Enter a normal string to simulate the message field or a JSON to simulate the whole message."
                 rows={5} />
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
          <MessageShowContainer>
            <MessageShow message={ruleSimulationResult} />
          </MessageShowContainer>
          )}
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
