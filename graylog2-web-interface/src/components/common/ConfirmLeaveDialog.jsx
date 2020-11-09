// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Prompt } from 'react-router-dom';
import { useCallback, useEffect } from 'react';

import AppConfig from 'util/AppConfig';

/**
 * This component should be conditionally rendered if you have a form that is in a "dirty" state. It will confirm with the user that they want to navigate away, refresh, or in any way unload the component.
 */
type Props = {
  question: string,
};
const ConfirmLeaveDialog = ({ question }: Props) => {
  const handleLeavePage = useCallback((e) => {
    if (AppConfig.gl2DevMode()) {
      return null;
    }

    e.returnValue = question;

    return question;
  }, [question]);

  useEffect(() => {
    window.addEventListener('beforeunload', handleLeavePage);

    return () => {
      window.removeEventListener('beforeunload', handleLeavePage);
    };
  }, [handleLeavePage]);

  return (
    <Prompt when={!AppConfig.gl2DevMode()} message={question} />
  );
};

ConfirmLeaveDialog.propTypes = {
  /** Phrase used in the confirmation dialog. */
  question: PropTypes.string,
};

ConfirmLeaveDialog.defaultProps = {
  question: 'Are you sure?',
};

/** @component */
export default ConfirmLeaveDialog;
