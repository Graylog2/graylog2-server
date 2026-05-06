import * as React from 'react';

import stringify from 'util/stringify';

type Props = { value: number | string | bigint };
const FieldValue = ({ value }: Props) => <>{stringify(value)}</>;
export default FieldValue;
