import useableCssProxy from 'css/useable-css-proxy';

describe('useable-css-proxy', () => {
  it('should return a proxy object', () => {
    expect(useableCssProxy).toBeDefined();
  });
  it('should return a proxy object for a key', () => {
    const result = useableCssProxy.container;
    expect(result).toBeDefined();
    expect(result.toString()).toEqual('container');
  });
});
