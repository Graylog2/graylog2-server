import * as React from 'react';
import { useMemo } from 'react';
import numeral from 'numeral';
import styled from 'styled-components';

type Props = {
  value: number,
}

const NumberCell = styled.span`
  float: right;
`;

const PercentageField = ({ value }: Props) => {
  const formatted = useMemo(() => numeral(value).format('0.00%'), [value]);

  return <NumberCell title={String(value)}>{formatted}</NumberCell>;
};

export default PercentageField;
