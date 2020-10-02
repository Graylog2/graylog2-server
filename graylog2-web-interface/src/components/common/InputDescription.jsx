// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { HelpBlock } from 'components/graylog';

const ErrorMessage = styled.span(({ theme }) => `
  color: ${theme.colors.variant.danger};
`);

const HelpMessage = styled.span(({ theme }) => `
  color: ${theme.colors.gray[50]};
`);

type Props = {
  help?: React.Node,
  error?: React.Node,
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
        <HelpMessage>
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
