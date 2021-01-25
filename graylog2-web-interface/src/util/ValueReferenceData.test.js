/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import ValueReferenceData from 'util/ValueReferenceData';

describe('ValueReferenceData', () => {
  let data = {};
  let valueReferenceData;

  beforeEach(() => {
    data = {
      title: {
        '@type': 'string',
        '@value': 'CP Alert Test',
      },
      disabled: {
        '@type': 'boolean',
        '@value': false,
      },
      matching_type: 'AND',
      test: false,
      testparam: {
        '@type': 'parameter',
        '@value': 'A_PARAM',
      },
      stream_rules: [
        {
          type: {
            '@type': 'string',
            '@value': 'EXACT',
          },
          value: {
            '@type': 'string',
            '@value': '598c2f4a8355e838edb19c88',
          },
          inverted: {
            '@type': 'boolean',
            '@value': true,
          },
          foo: 'bar',
          null_value: null,
        },
        {
          hello: 'world',
          test: {
            '@type': 'string',
            '@value': 'test1',
          },
        },
      ],
    };

    valueReferenceData = new ValueReferenceData(data);
  });

  /* eslint-disable dot-notation */
  it('creates all paths', () => {
    const paths = valueReferenceData.getPaths();

    expect(paths['title'].getValue()).toEqual('CP Alert Test');
    expect(paths['title'].getValueType()).toEqual('string');
    expect(paths['title'].getPath()).toEqual('title');
    expect(paths['title'].isValueRef()).toEqual(true);
    expect(paths['title'].isValueParameter()).toEqual(false);
    expect(paths['disabled'].getValue()).toEqual(false);
    expect(paths['disabled'].getValueType()).toEqual('boolean');
    expect(paths['disabled'].getPath()).toEqual('disabled');
    expect(paths['disabled'].isValueRef()).toEqual(true);
    expect(paths['disabled'].isValueParameter()).toEqual(false);
    expect(paths['matching_type'].getValue()).toEqual('AND');
    expect(paths['matching_type'].getValueType()).toEqual('string');
    expect(paths['matching_type'].getPath()).toEqual('matching_type');
    expect(paths['matching_type'].isValueRef()).toEqual(false);
    expect(paths['matching_type'].isValueParameter()).toEqual(false);
    expect(paths['test'].getValue()).toEqual(false);
    expect(paths['test'].getValueType()).toEqual('boolean');
    expect(paths['test'].getPath()).toEqual('test');
    expect(paths['test'].isValueRef()).toEqual(false);
    expect(paths['test'].isValueParameter()).toEqual(false);
    expect(paths['testparam'].getValue()).toEqual('A_PARAM');
    expect(paths['testparam'].getValueType()).toEqual('parameter');
    expect(paths['testparam'].getPath()).toEqual('testparam');
    expect(paths['testparam'].isValueRef()).toEqual(true);
    expect(paths['testparam'].isValueParameter()).toEqual(true);
    expect(paths['stream_rules.0.type'].getValue()).toEqual('EXACT');
    expect(paths['stream_rules.0.type'].getValueType()).toEqual('string');
    expect(paths['stream_rules.0.type'].getPath()).toEqual('stream_rules.0.type');
    expect(paths['stream_rules.0.type'].isValueRef()).toEqual(true);
    expect(paths['stream_rules.0.type'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.0.value'].getValue()).toEqual('598c2f4a8355e838edb19c88');
    expect(paths['stream_rules.0.value'].getValueType()).toEqual('string');
    expect(paths['stream_rules.0.value'].getPath()).toEqual('stream_rules.0.value');
    expect(paths['stream_rules.0.value'].isValueRef()).toEqual(true);
    expect(paths['stream_rules.0.value'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.0.inverted'].getValue()).toEqual(true);
    expect(paths['stream_rules.0.inverted'].getValueType()).toEqual('boolean');
    expect(paths['stream_rules.0.inverted'].getPath()).toEqual('stream_rules.0.inverted');
    expect(paths['stream_rules.0.inverted'].isValueRef()).toEqual(true);
    expect(paths['stream_rules.0.inverted'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.0.foo'].getValue()).toEqual('bar');
    expect(paths['stream_rules.0.foo'].getValueType()).toEqual('string');
    expect(paths['stream_rules.0.foo'].getPath()).toEqual('stream_rules.0.foo');
    expect(paths['stream_rules.0.foo'].isValueRef()).toEqual(false);
    expect(paths['stream_rules.0.foo'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.0.null_value'].getValue()).toEqual(null);
    expect(paths['stream_rules.0.null_value'].getValueType()).toEqual('object');
    expect(paths['stream_rules.0.null_value'].getPath()).toEqual('stream_rules.0.null_value');
    expect(paths['stream_rules.0.null_value'].isValueRef()).toEqual(false);
    expect(paths['stream_rules.0.null_value'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.1.hello'].getValue()).toEqual('world');
    expect(paths['stream_rules.1.hello'].getValueType()).toEqual('string');
    expect(paths['stream_rules.1.hello'].getPath()).toEqual('stream_rules.1.hello');
    expect(paths['stream_rules.1.hello'].isValueRef()).toEqual(false);
    expect(paths['stream_rules.1.hello'].isValueParameter()).toEqual(false);
    expect(paths['stream_rules.1.test'].getValue()).toEqual('test1');
    expect(paths['stream_rules.1.test'].getValueType()).toEqual('string');
    expect(paths['stream_rules.1.test'].getPath()).toEqual('stream_rules.1.test');
    expect(paths['stream_rules.1.test'].isValueRef()).toEqual(true);
    expect(paths['stream_rules.1.test'].isValueParameter()).toEqual(false);
  });

  it('can set values', () => {
    const paths = valueReferenceData.getPaths();

    expect(paths['disabled'].getValue()).toEqual(false);

    paths['disabled'].setValue(true);

    expect(paths['disabled'].getValue()).toEqual(true);

    expect(paths['stream_rules.0.value'].getValue()).toEqual('598c2f4a8355e838edb19c88');

    paths['stream_rules.0.value'].setValue('hello');

    expect(paths['stream_rules.0.value'].getValue()).toEqual('hello');

    expect(paths['stream_rules.0.foo'].getValue()).toEqual('bar');

    paths['stream_rules.0.foo'].setValue('yolo');

    expect(paths['stream_rules.0.foo'].getValue()).toEqual('yolo');
  });

  it('can set parameters on value references', () => {
    const paths = valueReferenceData.getPaths();

    expect(paths['disabled'].getValue()).toEqual(false);
    expect(paths['disabled'].getValueType()).toEqual('boolean');

    paths['disabled'].setParameter('PARAM');

    expect(paths['disabled'].getValue()).toEqual('PARAM');
    expect(paths['disabled'].getValueType()).toEqual('parameter');
  });

  it('cannot set parameters on non-value references', () => {
    const paths = valueReferenceData.getPaths();

    expect(paths['test'].isValueRef()).toEqual(false);

    expect(() => {
      paths['test'].setParameter('PARAM');
    }).toThrow();
  });
});
