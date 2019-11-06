const isLocalStorageReady = () => {
  if (typeof window === 'undefined') {
    return false;
  }

  const lsTestItem = 'gl-ls-test';

  try {
    localStorage.setItem(lsTestItem, lsTestItem);
    localStorage.removeItem(lsTestItem);
    return true;
  } catch (e) {
    return false;
  }
};

export default isLocalStorageReady;
