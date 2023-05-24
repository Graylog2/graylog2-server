import type { BlockDict, BlockFieldDict } from './types';

const getDictForFunction = (dict: BlockDict[], functionName: string) : BlockDict | undefined => (
  dict.find((entry) => entry.name === functionName)
);

const getDictForParam = (dict: BlockDict, paramName: string) : BlockFieldDict | undefined => (
  dict.params.find((param) => param.name === paramName)
);

export { getDictForFunction, getDictForParam };
