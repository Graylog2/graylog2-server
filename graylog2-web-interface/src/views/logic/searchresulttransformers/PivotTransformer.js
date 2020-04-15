export default (data) => data.map((result) => [result.name || result.id, result])
  .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});
