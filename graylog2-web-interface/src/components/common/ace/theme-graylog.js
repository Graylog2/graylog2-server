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
/* global ace */

ace.define('ace/theme/graylog', ['require', 'exports', 'module', 'ace/lib/dom'], (require, exports) => {
  // CSS found in `components/common/SourceCodeEditor.jsx`
  exports.cssText = ''; // eslint-disable-line no-param-reassign

  exports.cssClass = 'ace-graylog'; // eslint-disable-line no-param-reassign

  const dom = require('../lib/dom');
  dom.importCssString(exports.cssText, exports.cssClass);
});

(function () { // eslint-disable-line func-names
  ace.require(['ace/theme/graylog'], (m) => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
}());
