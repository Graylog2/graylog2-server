import React, { createContext, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import debounce from 'lodash/debounce';

import CombinedProvider from 'injection/CombinedProvider';

const { RulesActions } = CombinedProvider.get('Rules');
let VALIDATE_TIMEOUT;

export const PipelineRulesContext = createContext();

export const PipelineRulesProvider = ({ children, usedInPipelines, rule }) => {
  const [errorAnnotations, setErrorAnnotations] = useState([]);
  const [ruleSource, setRuleSource] = useState(rule?.source);
  const descriptionRef = useRef();
  const ruleSourceRef = useRef();

  useEffect(() => {
    descriptionRef.current.value = rule?.description;
  }, []);

  const createAnnotations = (nextErrors) => {
    const nextErrorAnnotations = nextErrors.map((e) => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    setErrorAnnotations(nextErrorAnnotations);
  };

  const validateNewRule = (nextSource, callback) => {
    const nextRule = {
      ...rule,
      source: nextSource,
      description: descriptionRef.current.value,
    };

    RulesActions.parse(nextRule, callback);
  };

  const validateBeforeSave = (callback = () => {}) => {
    const savedRule = {
      ...rule,
      source: ruleSource,
      description: descriptionRef.current.value,
    };

    RulesActions.parse(savedRule, () => callback(savedRule));
  };

  const handleChangeRuleSource = debounce((newRuleSource) => {
    setRuleSource(newRuleSource);

    if (VALIDATE_TIMEOUT) {
      clearTimeout(VALIDATE_TIMEOUT);
      VALIDATE_TIMEOUT = null;
    }

    VALIDATE_TIMEOUT = setTimeout(() => {
      validateNewRule(newRuleSource, (errors) => {
        const nextErrors = errors || [];

        createAnnotations(nextErrors);
      });
    }, 350);
  }, 150);

  const handleDescription = (newDescription) => {
    descriptionRef.current.value = newDescription;
  };

  const savePipelineRule = (nextRule, callback = () => {}) => {
    let promise;

    if (nextRule?.id) {
      promise = RulesActions.update.triggerPromise(nextRule);
    } else {
      promise = RulesActions.save.triggerPromise(nextRule);
    }

    promise.then(() => callback());
  };

  const handleSavePipelineRule = (callback = () => {}) => {
    validateBeforeSave((nextRule) => savePipelineRule(nextRule, callback));
  };

  return (
    <PipelineRulesContext.Provider value={{
      descriptionRef,
      errorAnnotations,
      handleChangeRuleSource,
      handleDescription,
      handleSavePipelineRule,
      ruleSource,
      usedInPipelines,
      validateBeforeSave,
      ruleSourceRef,
    }}>
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
    id: undefined,
    description: '',
    source: '',
  },
};
