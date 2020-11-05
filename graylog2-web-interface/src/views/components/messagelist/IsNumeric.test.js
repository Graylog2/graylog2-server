import isNumeric from './IsNumeric';

describe('isNumeric', () => {
  const testIsNumericString = ({ string, result }) => expect(isNumeric(string)).toEqual(result);

  it.each`
    string                                               | result
    ${undefined}                                         | ${false}
    ${null}                                              | ${false}
    ${''}                                                | ${false}
    ${'\n'}                                              | ${false}
    ${'foo bar'}                                         | ${false}
    ${'23'}                                              | ${true}
    ${'23.42'}                                           | ${true}
    ${23}                                                | ${true}
    ${23.42}                                             | ${true}
    ${'2020-11-02T09:37:55.256Z PUT /posts [200] 104ms'} | ${false}
  `('returns $result for value $string', testIsNumericString);
});
