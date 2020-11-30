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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { HelpBlock } from 'components/graylog';

const ErrorMessage = styled.span(({ theme }) => `
  color: ${theme.colors.variant.danger};
`);

const HelpMessage = styled.span(({ theme, hasError }) => `
  color: ${hasError ? theme.colors.gray[50] : 'inherit'};
`);

type Props = {
  help?: React.ReactElement,
  error?: React.ReactElement,
};

/**
 * Component that renders a help and error message for an input.
 * It always displays both messages.
 */
const InputDescription = ({ help, error }: Props) => {
  if (!help && !error) {
    return null;
  }

  return (
    <HelpBlock>
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
  help: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.string,
  ]),
  error: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.string,
  ]),
};

InputDescription.defaultProps = {
  help: undefined,
  error: undefined,
};

export default InputDescription;
