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
import styled, { css, StyledComponent } from 'styled-components';

import Icon from 'components/common/Icon';
import type { ThemeInterface } from 'theme/types';

type Props = {
  children: React.ReactNode,
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
    <table className="description-tooltips" style={{ marginBottom: '10px', display: 'inline' }}>
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
