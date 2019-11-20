import PaginationHelper from './PaginationHelper';

describe('PaginationHelper', () => {
  it('should create a pagination url without query', () => {
    const url = PaginationHelper.urlGenerator('https://foo', 1, 10);
    expect(url).toEqual('https://foo?page=1&per_page=10&resolve=true');
  });
  it('should create a pagination url with query', () => {
    const url = PaginationHelper.urlGenerator('https://foo', 1, 10, 'bar');
    expect(url).toEqual('https://foo?page=1&per_page=10&resolve=true&query=bar');
  });
  it('should create a pagination url with query and without resolve', () => {
    const url = PaginationHelper.urlGenerator('https://foo', 1, 10, 'bar', false);
    expect(url).toEqual('https://foo?page=1&per_page=10&resolve=false&query=bar');
  });
});
