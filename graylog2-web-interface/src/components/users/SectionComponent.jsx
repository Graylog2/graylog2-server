// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { Row, Col } from 'components/graylog';

type Props = {
  children: React.Node,
  title: string,
};

export const Headline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h2`
  margin-bottom: 15px;
`;

const SectionComponent = ({ children, title }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Headline>{title}</Headline>
      {children}
    </Col>
  </Row>
);

export default SectionComponent;
