import React, { createContext, useEffect, useRef, useCallback } from 'react';
import PropTypes from 'prop-types';

import CombinedProvider from 'injection/CombinedProvider';

const { RulesActions } = CombinedProvider.get('Rules');
let VALIDATE_TIMEOUT;

export const PipelineRulesContext = createContext();

export const PipelineRulesProvider = ({ children, usedInPipelines, rule }) => {
  const descriptionRef = useRef();
  const ruleSourceRef = useRef();

  const createAnnotations = (nextErrors) => {
    const nextErrorAnnotations = nextErrors.map((e) => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    ruleSourceRef.current.editor.getSession().setAnnotations(nextErrorAnnotations);
  };

  const validateNewRule = useCallback((callback) => {
    const nextRule = {
      ...rule,
      source: ruleSourceRef.current.editor.getSession().getValue(),
      description: descriptionRef.current.value,
    };

    RulesActions.parse(nextRule, callback);
  }, [rule]);

  const validateBeforeSave = (callback = () => {}) => {
    const savedRule = {
      ...rule,
      source: ruleSourceRef.current.editor.getSession().getValue(),
      description: descriptionRef.current.value,
    };

    RulesActions.parse(savedRule, () => callback(savedRule));
  };

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

    promise.then((response) => callback(response));
  };

  const handleSavePipelineRule = (callback = () => {}) => {
    validateBeforeSave((nextRule) => savePipelineRule(nextRule, callback));
  };

  useEffect(() => {
    if (ruleSourceRef.current) {
      ruleSourceRef.current.editor.getSession().setValue(rule.source, -1);
      ruleSourceRef.current.editor.session.setOption('useWorker', false);
    }

    if (descriptionRef.current) {
      descriptionRef.current.value = rule.description;
    }
  });

  useEffect(() => {
    if (ruleSourceRef.current) {
      ruleSourceRef.current.editor.on('change', () => {
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
      });
    }
  }, [validateNewRule]);

  return (
    <PipelineRulesContext.Provider value={{
      descriptionRef,
      handleDescription,
      handleSavePipelineRule,
      ruleSourceRef,
      usedInPipelines,
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
    description: '',
    source: '',
  },
};
