/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
const React = require('react');

const MockReactECharts = React.forwardRef(function MockReactECharts(props, ref) {
  const divRef = React.useRef(null);

  React.useImperativeHandle(ref, () => ({
    getEchartsInstance: () => ({
      getDom: () => divRef.current || document.createElement('div'),
      getZr: () => ({}),
      resize: () => {},
      dispose: () => {},
      on: () => {},
      off: () => {},
      convertFromPixel: () => {},
      convertToPixel: () => {},
      getOption: () => ({}),
    }),
  }));

  React.useEffect(() => {
    if (props.onChartReady) {
      props.onChartReady({
        getDom: () => divRef.current || document.createElement('div'),
        getZr: () => ({}),
        resize: () => {},
        dispose: () => {},
        on: () => {},
        off: () => {},
        convertFromPixel: () => {},
        convertToPixel: () => {},
        getOption: () => ({}),
      });
    }
  }, []);

  return React.createElement('div', { ref: divRef, 'data-testid': 'echart' });
});

MockReactECharts.displayName = 'MockReactECharts';

module.exports = MockReactECharts;
module.exports.default = MockReactECharts;
module.exports.__esModule = true;
