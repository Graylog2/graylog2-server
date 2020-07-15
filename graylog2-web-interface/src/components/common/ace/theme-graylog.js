/* global ace */

ace.define('ace/theme/graylog', ['require', 'exports', 'module', 'ace/lib/dom'], (require, exports) => {
  // CSS found in `components/common/SourceCodeEditor.jsx`
  exports.cssText = ''; // eslint-disable-line no-param-reassign

  exports.cssClass = 'ace-graylog'; // eslint-disable-line no-param-reassign

  const dom = require('../lib/dom');
  dom.importCssString(exports.cssText, exports.cssClass);
});

(function () {
  ace.require(['ace/theme/graylog'], (m) => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
}());
