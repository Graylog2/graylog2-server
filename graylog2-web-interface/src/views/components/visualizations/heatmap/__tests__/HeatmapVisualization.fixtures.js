const validData = {
  chart: [
    {
      key: ['00'],
      values: [
        { key: ['100', 'count()'], value: 217, rollup: false, source: 'col-leaf' },
        { key: ['304', 'count()'], value: 213, rollup: false, source: 'col-leaf' },
        { key: ['count()'], value: 430, rollup: true, source: 'row-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: ['01'],
      values: [
        { key: ['405', 'count()'], value: 230, rollup: false, source: 'col-leaf' },
        { key: ['201', 'count()'], value: 217, rollup: false, source: 'col-leaf' },
        { key: ['count()'], value: 447, rollup: true, source: 'row-leaf' },
      ],
      source: 'leaf',
    },
    {
      key: [],
      values: [
        { key: ['count()'], value: 877, rollup: true, source: 'row-inner' },
      ],
      source: 'non-leaf',
    },
  ],
};
// eslint-disable-next-line import/prefer-default-export
export { validData };
