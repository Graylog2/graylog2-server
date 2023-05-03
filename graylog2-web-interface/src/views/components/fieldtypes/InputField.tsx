import * as React from 'react';
import { useEffect } from 'react';

import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';
import type { ExtractStoreState } from 'stores/connect';
import { useStore } from 'stores/connect';

type Props = {
  value: string,
}

const mapInputs = (inputStore: ExtractStoreState<typeof InputsStore>) => {
  return Object.fromEntries(inputStore?.inputs?.map((input) => [input.id, input]) ?? []);
};

const InputField = ({ value }: Props) => {
  useEffect(() => { InputsActions.list(); }, []);
  const inputsMap = useStore(InputsStore, mapInputs);

  const inputTitle = inputsMap[value]?.title ?? value;

  return <span title={value}>{inputTitle}</span>;
};

export default InputField;
