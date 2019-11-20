// @flow strict
const PaginationHelper = {
  urlGenerator: (destUrl: string, page: number, perPage: number, query: string, resolve: boolean = true): string => {
    const url = `${destUrl}?page=${page}&per_page=${perPage}&resolve=${resolve.toString()}`;
    if (query) {
      return `${url}&query=${encodeURIComponent(query)}`;
    }
    return url;
  },
};

export default PaginationHelper;
