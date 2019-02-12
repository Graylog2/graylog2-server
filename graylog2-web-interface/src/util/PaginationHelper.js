const PaginationHelper = {
  urlGenerator: (destUrl, page, perPage, query, resolve = true) => {
    let url;
    if (query) {
      url = `${destUrl}?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}&resolve=${resolve}`;
    } else {
      url = `${destUrl}?page=${page}&per_page=${perPage}&resolve=${resolve}`;
    }
    return url;
  },
};

export default PaginationHelper;
