import React, { createContext, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import debounce from 'lodash/debounce';

import CombinedProvider from 'injection/CombinedProvider';

const { RulesActions } = CombinedProvider.get('Rules');

export const PipelineRulesContext = createContext();

export const PipelineRulesProvider = ({ children, usedInPipelines, rule }) => {
  const [errorAnnotations, setErrorAnnotations] = useState([]);
  const descriptionRef = useRef(rule?.description);
  const ruleSourceRef = useRef();

  console.log('descriptionRef', descriptionRef);
  console.log('ruleSourceRef', ruleSourceRef);

  const createAnnotations = (nextErrors) => {
    const nextErrorAnnotations = nextErrors.map((e) => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    setErrorAnnotations(nextErrorAnnotations);
  };

  const validateNewRule = (nextValue, callback) => {
    const nextRule = {
      ...rule,
      value: nextValue,
      description: descriptionRef.current.value,
    };

    RulesActions.parse(nextRule, callback);
  };

  const validateBeforeSave = (callback = () => {}) => {
    const savedRule = {
      ...rule,
      value: ruleSourceRef.current.value,
      description: descriptionRef.current.value,
    };

    RulesActions.parse(savedRule, callback);
  };

  const handleChangeRuleValue = debounce((newRuleValue) => {
    validateNewRule(newRuleValue, (errors) => {
      const nextErrors = errors || [];

      createAnnotations(nextErrors);
      ruleSourceRef.current.value = newRuleValue;
    });
  }, 500);

  const handleDescription = (newDescription) => {
    descriptionRef.current.value = newDescription;
    console.log('descriptionRef', descriptionRef);
  };

  const savePipelineRule = (callback = () => {}) => {
    let promise;

    if (rule?.id) {
      promise = RulesActions.update.triggerPromise(rule);
    } else {
      promise = RulesActions.save.triggerPromise(rule);
    }

    promise.then(() => callback());
  };

  const handleSavePipelineRule = (callback = () => {}) => {
    validateBeforeSave(() => savePipelineRule(callback));
  };

  return (
    <PipelineRulesContext.Provider value={{
      descriptionRef,
      errorAnnotations,
      handleChangeRuleValue,
      handleDescription,
      handleSavePipelineRule,
      ruleSourceRef,
      usedInPipelines,
      validateBeforeSave,
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
    value: PropTypes.string,
  }),
};

PipelineRulesProvider.defaultProps = {
  usedInPipelines: [],
  rule: {
    description: 'DO123',
    value: 'DA321',
  },
};
