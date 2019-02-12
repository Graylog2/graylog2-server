const PaginationHelper = {
  urlGenerator: (destUrl, page, perPage, query) => {
    let url;
    if (query) {
      url = `${destUrl}?page=${page}&per_page=${perPage}&query=${encodeURIComponent(query)}&resolve=true`;
    } else {
      url = `${destUrl}?page=${page}&per_page=${perPage}&resolve=true`;
    }
    return url;
  },
};

export default PaginationHelper;
