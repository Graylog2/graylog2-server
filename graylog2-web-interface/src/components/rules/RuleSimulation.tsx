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

  const handleRawMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawMessageToSimulate(event.target.value);
  };

  const handleRunRuleSimulation = () => {
    simulateRule(
      rawMessageToSimulate,
      currentRule || {
        ...rule,
        source: ruleSource,
      },
      setRuleSimulationResult,
    );
  };

  const handleResetRuleSimulation = () => {
    setRawMessageToSimulate('');
    setRuleSimulationResult(null);
    setStartRuleSimulation(false);
  };

  const handleStartRuleSimulation = () => {
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
                 placeholder="JSON"
                 value={rawMessageToSimulate}
                 onChange={handleRawMessageChange}
                 rows={5} />
          <Button bsStyle="info"
                  bsSize="xsmall"
                  disabled={!rawMessageToSimulate || !ruleSource}
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
