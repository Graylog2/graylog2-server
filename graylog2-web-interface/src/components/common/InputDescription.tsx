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
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { HelpBlock } from 'components/bootstrap';

const ErrorMessage = styled.span(({ theme }) => `
  color: ${theme.colors.variant.danger};
`);

const HelpMessage = styled.span<{ hasError: boolean }>(({ theme, hasError }) => `
  color: ${hasError ? theme.colors.gray[50] : 'inherit'};
`);

type Props = {
  className?: string,
  error?: React.ReactElement,
  help?: React.ReactNode,
};

/**
 * Component that renders a help and error message for an input.
 * It always displays both messages.
 */
const InputDescription = ({ className, error, help }: Props) => {
  if (!help && !error) {
    return null;
  }

  return (
    <HelpBlock className={className}>
      {error && (
        <ErrorMessage>
          {error}
        </ErrorMessage>
      )}
      {(!!error && !!help) && <br />}
      {help && (
        <HelpMessage hasError={!!error}>
          {help}
        </HelpMessage>
      )}
    </HelpBlock>
  );
};

InputDescription.propTypes = {
  className: PropTypes.string,
  error: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.string,
  ]),
  help: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.string,
  ]),
};

InputDescription.defaultProps = {
  className: undefined,
  error: undefined,
  help: undefined,
};

export default InputDescription;
