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
import React from 'react';
import styled, { css } from 'styled-components';

import type FetchError from 'logic/errors/FetchError';
import { Spinner, Icon } from 'components/common';
import type { ProcessingSteps } from 'components/inputs/InputSetupWizard/steps/StartInputStep';

type Props = {
  stepName: ProcessingSteps,
  isLoading: boolean,
  isSuccess: boolean,
  name?: string,
  isError: boolean,
  errorMessage?: FetchError,
}

const SuccessIcon = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.variant.success}
`);

const ErrorIcon = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.variant.danger}
`);

const ProgressMessage = ({ stepName, isLoading, isSuccess, isError, name = undefined, errorMessage = undefined } : Props) => {
  const loadingText: {[key in ProcessingSteps]: (entityName?: string) => string} = {
    createStream: (entityName) => `Creating Stream${entityName !== undefined ? (` "${entityName}"`) : ''}...`,
    startStream: (entityName) => `Starting Stream${entityName !== undefined ? (` "${entityName}"`) : ''}...`,
    createPipeline: (entityName) => `Creating Pipeline${entityName !== undefined ? (` "${entityName}"`) : ''}...`,
    setupRouting: (_) => 'Setting up routing...',
    startInput: (_) => 'Starting Input...',
  };

  const errorText: {[key in ProcessingSteps]: (name?: string) => string} = {
    createStream: (entityName) => `Creating Stream${entityName !== undefined ? (` "${entityName}"`) : ''} failed!`,
    startStream: (entityName) => `Starting Stream${entityName !== undefined ? (` "${entityName}"`) : ''} failed!`,
    createPipeline: (entityName) => `Creating Pipeline${entityName !== undefined ? (` "${entityName}"`) : ''} failed!`,
    setupRouting: (_) => 'Setting up routing failed!',
    startInput: (_) => "Input couldn't be started!",
  };

  const successText: {[key in ProcessingSteps]: (entityName?: string) => string} = {
    createStream: (entityName) => `Stream${entityName !== undefined ? (` "${entityName}"`) : ''} created!`,
    startStream: (entityName) => `Stream${entityName !== undefined ? (` "${entityName}"`) : ''} started!`,
    createPipeline: (entityName) => `Pipeline${entityName !== undefined ? (` "${entityName}"`) : ''} created!`,
    setupRouting: (_) => 'Routing set up!',
    startInput: (_) => 'Input started!',
  };

  if (isLoading) {
    return <p><Spinner text={loadingText[stepName](name)} /></p>;
  }

  if (isError) {
    return (
      <>
        <p><ErrorIcon name="close" title={errorText[stepName](name)} /> {errorText[stepName](name)}</p>
        {errorMessage && (<p><strong>Details:</strong> {errorMessage.message}</p>)}
      </>
    );
  }

  if (isSuccess) {
    return (
      <p><SuccessIcon name="check" title={successText[stepName](name)} /> {successText[stepName](name)}</p>
    );
  }

  return null;
};

export default ProgressMessage;
