// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';

import { Col, Row, HelpBlock } from 'components/graylog';

type Props = {
  label: React.Node,
  value: ?React.Node,
  help?: string,
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

const ReadOnlyFormGroup = ({ label, value, help }: Props) => (
  <Row>
    <LabelCol sm={3}>
      {label}
    </LabelCol>
    <ValueCol sm={9}>
      {value ?? '-'}
      {help && <HelpBlock>{help}</HelpBlock>}
    </ValueCol>
  </Row>
);

ReadOnlyFormGroup.defaultProps = {
  help: undefined,
};

export default ReadOnlyFormGroup;
