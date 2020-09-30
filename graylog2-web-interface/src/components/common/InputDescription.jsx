// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { HelpBlock } from 'components/graylog';

const Wrapper: StyledComponent<{hasError: boolean}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme, hasError }) => hasError && `
  color: ${theme.colors.variant.danger};
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
    <Wrapper hasError={!!error}>
      <HelpBlock>
        {error}
        {(error && help) && <br />}
        {help}
      </HelpBlock>
    </Wrapper>
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
