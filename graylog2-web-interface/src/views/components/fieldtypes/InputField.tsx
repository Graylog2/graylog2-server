import * as React from 'react';
import { useContext } from 'react';

import InputsContext from 'contexts/InputsContext';

type Props = {
  value: string,
}

const InputField = ({ value }: Props) => {
  const inputsMap = useContext(InputsContext);

  const inputTitle = inputsMap[value]?.title ?? value;

  return <span title={value}>{inputTitle}</span>;
};

export default InputField;
