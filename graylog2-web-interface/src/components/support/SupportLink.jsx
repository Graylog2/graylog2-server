// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import Icon from 'components/common/Icon';
import type { ThemeInterface } from 'theme/types';

type Props = {
  children: React.Node,
  small: boolean,
};

const IconStack: StyledComponent<{small: boolean}, ThemeInterface, HTMLDivElement> = styled.div(({ small, theme }) => css`
  position: relative;
  min-width: 2.5em;
  font-size: ${small ? theme.fonts.size.body : theme.fonts.size.large};
  
  .fa-stack-1x {
    color: ${theme.colors.global.textAlt};
  }
  
  .fa-stack-2x {
    color: ${theme.colors.global.textDefault};
  }
`);

const SupportLink = ({ small, children }: Props) => {
  return (
    <table className="description-tooltips" style={{ marginBottom: '10px' }}>
      <tbody>
        <tr>
          <td style={{ width: '40px' }}>
            <IconStack className={`fa-stack ${small ? '' : 'fa-lg'}`} small={small}>
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
