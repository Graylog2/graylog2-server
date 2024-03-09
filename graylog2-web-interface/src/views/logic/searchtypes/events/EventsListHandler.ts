export default {
  convert({ id, events, total_results, type }) {
    return {
      id,
      events,
      totalResults: total_results,
      type,
    };
  },
};
