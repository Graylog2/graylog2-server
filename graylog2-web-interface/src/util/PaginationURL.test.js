import PaginationURL from './PaginationURL';

describe('PaginationUR', () => {
  it('should create a pagination url without query', () => {
    const url = PaginationURL('https://foo', 1, 10);
    expect(url).toEqual('https://foo/?page=1&per_page=10');
  });
  it('should create a pagination url with query', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar');
    expect(url).toEqual('https://foo/?page=1&per_page=10&query=bar');
  });
  it('should create a pagination url with query addition field', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar', { resolve: false });
    expect(url).toEqual('https://foo/?page=1&per_page=10&resolve=false&query=bar');
  });
  it('should create a pagination url with query addition fields', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar',
      { bool: false, number: 12, string: 'string', double: 1.2, object: { toString: () => 'object' } });
    expect(url).toEqual('https://foo/?page=1&per_page=10&bool=false&number=12&string=string&double=1.2&object=object&query=bar');
  });
});
