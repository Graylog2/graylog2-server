// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { Icon } from 'components/common';
import type { ThemeInterface } from 'theme';

type Props = {
  children: React.Node,
};

const Description: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: inline-flex;
  justify-content: center;
  align-items: center;
`;

const IconStack: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  position: relative;
  min-width: 2.5em;
  
  .fa-stack-1x {
    color: ${theme.colors.global.textAlt};
  }
  
  .fa-stack-2x {
    color: ${theme.colors.global.textDefault};
  }
`);

const Content: StyledComponent<{}, void, HTMLParagraphElement> = styled.p`
  font-weight: bold;
  margin: 0;
`;

const SmallSupportLink = ({ children }: Props) => {
  return (
    <Description className="description-tooltips">
      <IconStack className="fa-stack">
        <Icon name="circle" className="fa-stack-2x" />
        <Icon name="lightbulb" className="fa-stack-1x" type="regular" />
      </IconStack>

      <Content>
        {children}
      </Content>
    </Description>
  );
};

SmallSupportLink.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SmallSupportLink;
