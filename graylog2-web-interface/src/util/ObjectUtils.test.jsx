import ObjectUtils from 'util/ObjectUtils';

describe('ObjectUtils', () => {
  let testObj = {};

  beforeEach(() => {
    testObj = {
      a: {
        b: 1,
        c: 2,
        d: {
          e: {
            f: 'test',
          },
          v: [{ a: 1, c: { d: 5 } }],
        },
      },
      g: 3,
      h: {
        i: 'test2',
      },
      j: [{ a: 0 }, 'b'],
    };
  });

  it('should get all paths of a object', () => {
    const expectedPaths = [
      'a.b',
      'a.c',
      'a.d.e.f',
      'a.d.v[0].a',
      'a.d.v[0].c.d',
      'g',
      'h.i',
      'j[0].a',
      'j[1]',
    ];
    const paths = ObjectUtils.getPaths(testObj);
    expect(paths).toEqual(expectedPaths);
  });

  it('should get value of testObj', () => {
    expect(ObjectUtils.getValue(testObj, 'a.b')).toEqual(1);
    expect(ObjectUtils.getValue(testObj, 'a.c')).toEqual(2);
    expect(ObjectUtils.getValue(testObj, 'a.d')).toEqual({ e: { f: 'test' }, v: [{ a: 1, c: { d: 5 } }] });
    expect(ObjectUtils.getValue(testObj, 'g')).toEqual(3);
    expect(ObjectUtils.getValue(testObj, 'h.i')).toEqual('test2');
    expect(ObjectUtils.getValue(testObj, 'j[0].a')).toEqual(0);
    expect(ObjectUtils.getValue(testObj, 'j[1]')).toEqual('b');
    expect(ObjectUtils.getValue(testObj, 'a.d.v[0].c.d')).toEqual(5);
    expect(ObjectUtils.getValue(testObj, 'xxx.xxx')).toEqual(undefined);
  });

  it('should set value of testObj', () => {
    ObjectUtils.setValue(testObj, 'a.b', 3);
    expect(ObjectUtils.getValue(testObj, 'a.b')).toEqual(3);

    ObjectUtils.setValue(testObj, 'a.c', { a: 3 });
    expect(ObjectUtils.getValue(testObj, 'a.c')).toEqual({ a: 3 });

    ObjectUtils.setValue(testObj, 'a.d.v[0].c.d', 123);
    expect(ObjectUtils.getValue(testObj, 'a.d.v[0].c.d')).toEqual(123);

    ObjectUtils.setValue(testObj, 'a.d', 'testString');
    expect(ObjectUtils.getValue(testObj, 'a.d')).toEqual('testString');

    ObjectUtils.setValue(testObj, 'g', undefined);
    expect(ObjectUtils.getValue(testObj, 'g')).toEqual(undefined);

    ObjectUtils.setValue(testObj, 'h.i', 'h.i');
    expect(ObjectUtils.getValue(testObj, 'h.i')).toEqual('h.i');

    ObjectUtils.setValue(testObj, 'j[0].a', '3');
    expect(ObjectUtils.getValue(testObj, 'j[0].a')).toEqual('3');

    ObjectUtils.setValue(testObj, 'j[1]', 3);
    expect(ObjectUtils.getValue(testObj, 'j[1]')).toEqual(3);
  });

  it('should set every value a getPath can find', () => {
    const allPaths = ObjectUtils.getPaths(testObj);
    expect(allPaths.length).toEqual(9);
    allPaths.forEach(path => ObjectUtils.setValue(testObj, path, 1));
    allPaths.forEach(path => expect(ObjectUtils.getValue(testObj, path)).toEqual(1));
    const allPathsAfter = ObjectUtils.getPaths(testObj);
    expect(allPathsAfter.length).toEqual(9);
  });
});
