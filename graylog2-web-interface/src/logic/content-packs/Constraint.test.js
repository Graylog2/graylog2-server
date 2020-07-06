import { Set } from 'immutable';

import Constraint from './Constraint';

describe('Constraint', () => {
  const constraint1 = Constraint.builder()
    .type('server')
    .version('3.0.0')
    .build();

  const constraint2 = Constraint.builder()
    .type('server')
    .version('3.0.0')
    .build();

  const constraint3 = Constraint.builder()
    .type('plugin')
    .version('3.0.0')
    .plugin('graylog.plugin.foo')
    .build();

  it('should be add to a Set without duplication', () => {
    const set = Set().add(constraint1)
      .add(constraint2)
      .add(constraint3);

    expect(set.size).toBe(2);
  });

  it('should be equaling it self', () => {
    expect(constraint1.equals(constraint2)).toBe(true);
    expect(constraint1.equals(constraint3)).toBe(false);
    expect(constraint1.equals({ version: '3.0.0', type: 'server', plugin: 'server' })).toBe(true);
  });
});
