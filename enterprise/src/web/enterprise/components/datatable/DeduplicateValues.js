export const deduplicateValues = (rows, rowFieldNames) => {
  const duplicateKeys = {};
  return rows.map((item) => {
    const reducedItem = Object.assign({}, item);
    const entries = Object.entries(reducedItem);
    entries.forEach(([key, value], entryIdx) => {
      if (!rowFieldNames.includes(key)) {
        return;
      }
      if (duplicateKeys[key] === value) {
        delete reducedItem[key];
      } else {
        entries.slice(entryIdx + 1)
          .forEach(entry => delete duplicateKeys[entry[0]]);
        duplicateKeys[key] = value;
      }
    });
    return reducedItem;
  });
};
