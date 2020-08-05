import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import Icon from 'components/common/Icon';

const IconStack = styled.span(({ small, theme }) => css`
  font-size: ${small ? theme.fonts.size.body : theme.fonts.size.large};
`);

const SupportLink = ({ small, children }) => {
  return (
    <table className="description-tooltips" style={{ marginBottom: '10px', display: 'inline' }}>
      <tbody>
        <tr>
          <td style={{ width: '40px' }}>
            <IconStack className={`fa-stack ${!small && 'fa-lg'}`} small={small}>
              <Icon name="circle" className="fa-stack-2x" />
              <Icon name="lightbulb" className="fa-stack-1x" inverse />
            </IconStack>
          </td>
          <td>
            <strong>
              {children}
            </strong>
          </td>
        </tr>
      </tbody>
    </table>
  );
};

SupportLink.propTypes = {
  small: PropTypes.bool,
  children: PropTypes.node.isRequired,
};

SupportLink.defaultProps = {
  small: false,
};

export default SupportLink;
