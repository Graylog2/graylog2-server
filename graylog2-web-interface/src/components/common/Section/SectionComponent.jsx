// @flow strict
import * as React from 'react';
import styled, { type StyledComponent, css } from 'styled-components';

import Spinner from 'components/common/Spinner';
import { type ThemeInterface } from 'theme';
import { Row, Col } from 'components/graylog';

type Props = {
  children: React.Node,
  title: string,
  showLoading?: boolean,
  headerActions?: React.Node,
};

const Header = styled.div`
  display: flex;
  justify-content: space-between;

  * + * {
   margin-left: 10px;
 }
`;

export const Headline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h2`
  margin-bottom: 15px;
  display: inline;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const SectionComponent = ({ children, title, showLoading = false, headerActions }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Header>
        <Headline>
          {title}
          {showLoading && <LoadingSpinner text="" delay={0} />}
        </Headline>
        {headerActions}
      </Header>
      {children}
    </Col>
  </Row>
);

SectionComponent.defaultProps = {
  showLoading: false,
  headerActions: undefined,
};

export default SectionComponent;
