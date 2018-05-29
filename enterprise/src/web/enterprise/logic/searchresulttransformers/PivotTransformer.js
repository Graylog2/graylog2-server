export default (data) => {
  if (data && data[0] && data[0].rows) {
    return data[0].rows;
  }
  return [];
}