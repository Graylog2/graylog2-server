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
  stepName?: ProcessingSteps;
  isLoading: boolean;
  isSuccess: boolean;
  name?: string;
  isError: boolean;
  errorMessage?: FetchError;
  title?: string;
  details?: string;
};

const SuccessIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.success};
  `,
);

const ErrorIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.danger};
  `,
);

const ProgressMessage = ({
  stepName = undefined,
  isLoading,
  isSuccess,
  isError,
  name = undefined,
  errorMessage = undefined,
  title = undefined,
  details = undefined,
}: Props) => {
  const loadingText: { [key in ProcessingSteps]: (entityName?: string) => string } = {
    createStream: (entityName) => `Creating Stream${entityName !== undefined ? ` "${entityName}"` : ''}...`,
    startStream: (entityName) => `Starting Stream${entityName !== undefined ? ` "${entityName}"` : ''}...`,
    createPipeline: (entityName) => `Creating Pipeline${entityName !== undefined ? ` "${entityName}"` : ''}...`,
    setupRouting: (_) => 'Setting up routing...',
    result: (_) => '',
    deleteStream: (entityName) => `Deleting Stream${entityName !== undefined ? ` "${entityName}"` : ''}...`,
    deletePipeline: (entityName) => `Deleting Pipeline${entityName !== undefined ? ` "${entityName}"` : ''}...`,
    deleteRouting: (_) => 'Removing routing...',
    connectPipeline: (_) => 'Connecting pipeline to stream...',
  };

  const errorText: { [key in ProcessingSteps]: (name?: string) => string } = {
    createStream: (entityName) => `Creating Stream${entityName !== undefined ? ` "${entityName}"` : ''} failed!`,
    startStream: (entityName) => `Starting Stream${entityName !== undefined ? ` "${entityName}"` : ''} failed!`,
    createPipeline: (entityName) => `Creating Pipeline${entityName !== undefined ? ` "${entityName}"` : ''} failed!`,
    setupRouting: (_) => 'Setting up routing failed!',
    result: (_) => 'Starting the Input has failed. Please roll it back to clean it up.',
    deleteStream: (entityName) => `Deleting Stream${entityName !== undefined ? ` "${entityName}"` : ''} failed!`,
    deletePipeline: (entityName) => `Deleting Pipeline${entityName !== undefined ? ` "${entityName}"` : ''} failed!`,
    deleteRouting: (_) => 'Removing routing failed!',
    connectPipeline: (_) => 'Connecting pipeline to stream failed!',
  };

  const successText: { [key in ProcessingSteps]: (entityName?: string) => string } = {
    createStream: (entityName) => `Stream${entityName !== undefined ? ` "${entityName}"` : ''} created!`,
    startStream: (entityName) => `Stream${entityName !== undefined ? ` "${entityName}"` : ''} started!`,
    createPipeline: (entityName) => `Pipeline${entityName !== undefined ? ` "${entityName}"` : ''} created!`,
    setupRouting: (_) => 'Routing set up!',
    result: (_) => 'Input started successfully!',
    deleteStream: (entityName) => `Stream${entityName !== undefined ? ` "${entityName}"` : ''} deleted!`,
    deletePipeline: (entityName) => `Pipeline${entityName !== undefined ? ` "${entityName}"` : ''} deleted!`,
    deleteRouting: (_) => 'Routing removed!',
    connectPipeline: (_) => 'Pipeline connected to stream successfully!',
  };

  if (isLoading) {
    return (
      <p>
        <Spinner text={title ?? loadingText[stepName](name)} />
      </p>
    );
  }

  if (isError) {
    return (
      <>
        <p>
          <ErrorIcon name="close" title={title ?? errorText[stepName](name)} />
          {title ?? errorText[stepName](name)}
        </p>
        {details ||
          (errorMessage && (
            <p>
              <strong>Details:</strong> {details ?? errorMessage.message}
            </p>
          ))}
      </>
    );
  }

  if (isSuccess) {
    return (
      <p>
        <SuccessIcon name="check" title={title ?? successText[stepName](name)} />
        {title ?? successText[stepName](name)}
      </p>
    );
  }

  return null;
};

export default ProgressMessage;
