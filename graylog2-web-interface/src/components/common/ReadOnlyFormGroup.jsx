// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { Col, Row, HelpBlock } from 'components/graylog';
import Icon from 'components/common/Icon';

type Props = {
  label: React.Node,
  value: ?mixed,
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

const BooleanIcon: StyledComponent<{value?: boolean}, ThemeInterface, Icon> = styled(Icon)(({ theme, value }) => `
  color: ${value ? theme.colors.variant.success : theme.colors.variant.danger};
`);

const BooleanValue = ({ value }: { value: boolean }) => (
  <><BooleanIcon name={value ? 'check-circle' : 'times-circle'} value={value} /> {value ? 'yes' : 'no'}</>
);

const readableValue = (value: $PropertyType<Props, 'value'>) => {
  if (typeof value === 'boolean') {
    return <BooleanValue value={value} />;
  }

  if (value) {
    return value;
  }

  return '-';
};

const ReadOnlyFormGroup = ({ label, value, help }: Props) => (
  <Row>
    <LabelCol sm={3}>
      {label}
    </LabelCol>
    <ValueCol sm={9}>
      {readableValue(value)}
      {help && <HelpBlock>{help}</HelpBlock>}
    </ValueCol>
  </Row>
);

ReadOnlyFormGroup.defaultProps = {
  help: undefined,
};

export default ReadOnlyFormGroup;
