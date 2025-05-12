import { useStore } from 'stores/connect';
import { InputTypesStore } from 'stores/inputs/InputTypesStore';

const useInputTypes = () => {
  const { inputTypes } = useStore(InputTypesStore);

  return inputTypes;
};

export default useInputTypes;
