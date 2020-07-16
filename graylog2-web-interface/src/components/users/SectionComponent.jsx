// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import Spinner from 'components/common/Spinner';
import { type ThemeInterface } from 'theme';
import { Row, Col } from 'components/graylog';

type Props = {
  children: React.Node,
  title: string,
  showLoading?: boolean,
};

export const Headline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h2`
  margin-bottom: 15px;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => `
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const SectionComponent = ({ children, title, showLoading = false }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Headline>{title}{showLoading && <LoadingSpinner text="" delay={0} />}</Headline>
      {children}
    </Col>
  </Row>
);

SectionComponent.defaultProps = {
  showLoading: false,
};

export default SectionComponent;
