import { debounce } from 'lodash';

type PromiseReturnType<T> = T extends (...args: any[]) => Promise<infer R> ? R : never;

const debouncedValidateQuery = <T extends (...args: any[]) => Promise<any>>(fn: T, delay: number) => {
  const debouncedFn = debounce((resolve: PromiseReturnType<T>, ...args: Parameters<T>) => fn(...args).then(resolve), delay);

  return (...args: Parameters<T>) => new Promise<PromiseReturnType<T>>((resolve: PromiseReturnType<T>) => {
    debouncedFn(resolve, ...args);
  });
};

export default debouncedValidateQuery;
