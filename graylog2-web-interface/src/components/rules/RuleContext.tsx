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
import React, { createContext, useEffect, useRef, useCallback, useState, useMemo } from 'react';

import type { RuleType } from 'stores/rules/RulesStore';
import { RulesActions } from 'stores/rules/RulesStore';
import { getSavedRuleSourceCode, removeSavedRuleSourceCode } from 'hooks/useRuleBuilder';

import { jsonifyText } from './rule-builder/helpers';

let VALIDATE_TIMEOUT;

export const DEFAULT_SIMULATOR_JSON_MESSAGE = 'message: test\nsource: unknown\n';

export const PipelineRulesContext = createContext(undefined);

export enum SimulationFieldType {
  Simple = 'Simple',
  KeyValue = 'KeyValue',
  JSON = 'JSON',
}

const getMessageToSimulate = (rawMessage: string, messageType: SimulationFieldType) => {
  switch (messageType) {
    case SimulationFieldType.JSON:
    case SimulationFieldType.KeyValue:
      return jsonifyText(rawMessage);
    case SimulationFieldType.Simple:
    default:
      return JSON.stringify({ message: rawMessage });
  }
};

const savePipelineRule = (nextRule: RuleType, callback: (rule: RuleType) => void = () => {}, onError: (error: object) => void = () => {}) => {
  let promise;

  if (nextRule?.id) {
    promise = RulesActions.update(nextRule);
  } else {
    promise = RulesActions.save(nextRule);
  }

  promise.then(callback).catch(onError);
};

type Props = {
  children: React.ReactNode,
  usedInPipelines?: Array<string>
  rule?: RuleType
}

export const PipelineRulesProvider = ({ children, usedInPipelines = [], rule }: Props) => {
  const ruleSourceRef = useRef(undefined);
  const [, setAceLoaded] = useState(false);
  const [ruleSource, setRuleSource] = useState(rule?.source);
  const [description, setDescription] = useState(rule?.description);
  const [rawMessageToSimulate, setRawMessageToSimulate] = useState('');
  const [ruleSimulationResult, setRuleSimulationResult] = useState(null);

  useEffect(() => {
    const savedSourceCode = getSavedRuleSourceCode();
    setRuleSource(savedSourceCode || rule?.source);
    setDescription(rule?.description);
    removeSavedRuleSourceCode();

    if (rule?.simulator_message) {
      setRawMessageToSimulate(rule?.simulator_message);
    }
  }, [rule]);

  const createAnnotations = useCallback((nextErrors: Array<{ line: number, position_in_line: number, reason: string }>) => {
    const nextErrorAnnotations = nextErrors.map((e) => ({ row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' }));

    ruleSourceRef?.current?.editor?.getSession().setAnnotations(nextErrorAnnotations);
  }, []);

  const validateNewRule = useCallback((callback) => {
    const nextRule = {
      ...rule,
      source: ruleSourceRef?.current?.editor?.getSession().getValue(),
      simulator_message: rawMessageToSimulate,
      description,
    };

    RulesActions.parse(nextRule, callback);
  }, [rule, description, rawMessageToSimulate]);

  const simulateRule = useCallback((_rule: RuleType, simulationType: SimulationFieldType, messageString: string = rawMessageToSimulate, callback: React.Dispatch<any> | (() => void) = setRuleSimulationResult) => {
    const messageToSimulate = getMessageToSimulate(messageString, simulationType);
    RulesActions.simulate(messageToSimulate, _rule, callback);
  }, [rawMessageToSimulate, setRuleSimulationResult]);

  useEffect(() => {
    if (ruleSourceRef?.current) {
      ruleSourceRef?.current?.editor?.session.setOption('useWorker', false);
    }
  });

  const pipelineRulesContextValue = useMemo(() => {
    const validateBeforeSave = (callback: (nextRule: RuleType) => void = () => {}) => {
      const savedRule = {
        ...rule,
        source: ruleSourceRef?.current?.editor?.getSession().getValue(),
        simulator_message: rawMessageToSimulate,
        description,
      };

      RulesActions.parse(savedRule, () => callback(savedRule));
    };

    const handleSavePipelineRule = (callback: (rule: RuleType) => void = () => {}, onError: (error: object) => void = () => {}) => {
      validateBeforeSave((nextRule) => savePipelineRule(nextRule, callback, onError));
    };

    const onChangeSource = (source: string) => {
      setRuleSource(source);

      if (VALIDATE_TIMEOUT) {
        clearTimeout(VALIDATE_TIMEOUT);
        VALIDATE_TIMEOUT = null;
      }

      VALIDATE_TIMEOUT = setTimeout(() => {
        validateNewRule((errors) => {
          const nextErrors = errors || [];

          createAnnotations(nextErrors);
        });
      }, 500);
    };

    return ({
      rule: {
        ...rule,
        description,
        source: ruleSource,
        simulator_message: rawMessageToSimulate,
      },
      description,
      handleDescription: setDescription,
      handleSavePipelineRule,
      ruleSourceRef,
      usedInPipelines,
      onAceLoaded: () => setAceLoaded(true),
      ruleSource,
      onChangeSource,
      simulateRule,
      rawMessageToSimulate,
      setRawMessageToSimulate,
      ruleSimulationResult,
      setRuleSimulationResult,
    });
  }, [
    description,
    createAnnotations,
    rule,
    ruleSource,
    usedInPipelines,
    validateNewRule,
    simulateRule,
    rawMessageToSimulate,
    ruleSimulationResult,
  ]);

  return (
    <PipelineRulesContext.Provider value={pipelineRulesContextValue}>
      {children}
    </PipelineRulesContext.Provider>
  );
};
