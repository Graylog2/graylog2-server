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
// eslint-disable-next-line no-undef
ace.define('ace/theme/ace-queryinput', ['require', 'exports', 'module', 'ace/lib/dom'], (acequire, exports) => {
  /* eslint-disable no-param-reassign */
  exports.isDark = false;

  exports.cssClass = 'ace-queryinput';
  /* eslint-enable no-param-reassign */

  const dom = acequire('../lib/dom');

  dom.importCssString(exports.cssText, exports.cssClass);
});
