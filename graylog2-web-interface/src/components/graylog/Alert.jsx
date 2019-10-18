import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { Alert as BootstrapAlert } from 'react-bootstrap';

import { useTheme } from 'theme/GraylogThemeContext';
import bsStyleVariant from './variants/bsStyle';

const Alert = ({ bsStyle, ...props }) => {
  const alertStyles = () => {
    const { utility } = useTheme();

    const cssBuilder = (hex) => {
      const borderColor = utility.mix(hex, -10);
      const backgroundColor = utility.mix(hex, -75);
      const textColor = utility.mix(hex, 60);

      return css`
        background-color: ${backgroundColor};
        border-color: ${borderColor};
        color: ${textColor};
      `;
    };

    return bsStyleVariant(cssBuilder);
  };

  const StyledAlert = useCallback(styled(BootstrapAlert)`
    ${alertStyles()}
  `, [bsStyle]);

  return (
    <StyledAlert bsStyle={bsStyle} {...props} />
  );
};

Alert.propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

Alert.defaultProps = {
  bsStyle: 'default',
};

export default Alert;
