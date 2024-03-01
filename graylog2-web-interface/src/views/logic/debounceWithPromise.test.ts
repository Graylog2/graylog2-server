import debounceWithPromise from './debounceWithPromise';

describe('debounceWithPromise', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  it('never returns a pending promise', async () => {
    const fn = jest.fn(async (attempt: number) => attempt);

    const debouncedFn = debounceWithPromise(fn, 300);

    const result1 = debouncedFn(1);
    const result2 = debouncedFn(2);
    const result3 = debouncedFn(3);

    jest.advanceTimersByTime(400);

    await expect(result1).resolves.toBe(3);
    await expect(result2).resolves.toBe(3);
    await expect(result3).resolves.toBe(3);
  });
});
