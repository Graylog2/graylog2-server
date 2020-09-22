// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { HelpBlock } from 'components/graylog';

const StyledHelpBlock: StyledComponent<{hasError: boolean}, ThemeInterface, typeof HelpBlock> = styled(HelpBlock)(({ theme, hasError }) => `
  ${hasError && `
    color: ${theme.colors.variant.danger};
  `};
`);

const InputDescription = ({ help, error }) => {
  if (!help && !error) {
    return null;
  }

  return (
    <StyledHelpBlock hasError={!!error}>
      {error && <>{error}<br /></>}
      {help}
    </StyledHelpBlock>
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
