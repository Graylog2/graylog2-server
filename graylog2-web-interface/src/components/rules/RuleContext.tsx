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
import PropTypes from 'prop-types';

import type { RuleType } from 'stores/rules/RulesStore';
import { RulesActions } from 'stores/rules/RulesStore';

let VALIDATE_TIMEOUT;

export const PipelineRulesContext = createContext(undefined);

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
  usedInPipelines: Array<string>,
  rule: RuleType,
}

export const PipelineRulesProvider = ({ children, usedInPipelines, rule }: Props) => {
  const descriptionRef = useRef(undefined);
  const ruleSourceRef = useRef(undefined);
  const [, setAceLoaded] = useState(false);
  const [ruleSource, setRuleSource] = useState(rule.source);

  const createAnnotations = useCallback((nextErrors: Array<{ line: number, position_in_line: number, reason: string }>) => {
    const nextErrorAnnotations = nextErrors.map((e) => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    ruleSourceRef.current.editor.getSession().setAnnotations(nextErrorAnnotations);
  }, []);

  const validateNewRule = useCallback((callback) => {
    const nextRule = {
      ...rule,
      source: ruleSourceRef.current.editor.getSession().getValue(),
      description: descriptionRef.current.value,
    };

    RulesActions.parse(nextRule, callback);
  }, [rule]);

  useEffect(() => {
    if (ruleSourceRef.current) {
      ruleSourceRef.current.editor.session.setOption('useWorker', false);
    }

    if (descriptionRef.current) {
      descriptionRef.current.value = rule.description;
    }
  });

  const pipelineRulesContextValue = useMemo(() => {
    const validateBeforeSave = (callback: (nextRule: RuleType) => void = () => {}) => {
      const savedRule = {
        ...rule,
        source: ruleSourceRef.current.editor.getSession().getValue(),
        description: descriptionRef.current.value,
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
      descriptionRef,
      handleDescription: (newDescription) => { descriptionRef.current.value = newDescription; },
      handleSavePipelineRule,
      ruleSourceRef,
      usedInPipelines,
      onAceLoaded: () => setAceLoaded(true),
      ruleSource,
      onChangeSource,
    });
  }, [
    createAnnotations,
    rule,
    ruleSource,
    usedInPipelines,
    validateNewRule,
  ]);

  return (
    <PipelineRulesContext.Provider value={pipelineRulesContextValue}>
      {children}
    </PipelineRulesContext.Provider>
  );
};

PipelineRulesProvider.propTypes = {
  children: PropTypes.node.isRequired,
  usedInPipelines: PropTypes.array,
  rule: PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string,
    description: PropTypes.string,
    source: PropTypes.string,
  }),
};

PipelineRulesProvider.defaultProps = {
  usedInPipelines: [],
  rule: {
    description: '',
    source: '',
  },
};
