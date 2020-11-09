// @flow strict
import numeral from 'numeral';

const formatNumber = (value: number): string => numeral(value).format('0,0.[0000000]');

export default formatNumber;
