// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';

import { Col, Row } from 'components/graylog';

type Props = {
  label: React.Node,
  value: React.Node,
};

const ValueCol = styled(Col)`
  padding-top: 7px;
`;

const LabelCol = styled(ValueCol)(({ theme }) => css`
  font-weight: bold;

  @media (min-width: ${theme.breakpoints.min.md}) {
    text-align: right;
  }
`);

const ReadOnlyFormField = ({ label, value }: Props) => (
  <Row>
    <LabelCol sm={3}>
      {label}
    </LabelCol>
    <ValueCol sm={9}>
      {value ?? '-'}
    </ValueCol>
  </Row>
);

export default ReadOnlyFormField;
