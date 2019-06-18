// eslint-disable-next-line import/no-webpack-loader-syntax
import css from '!raw!./ace-queryinput.css';

// eslint-disable-next-line no-undef
ace.define('ace/theme/ace-queryinput', ['require', 'exports', 'module', 'ace/lib/dom'], (acequire, exports) => {
  /* eslint-disable no-param-reassign */
  exports.isDark = false;
  exports.cssClass = 'ace-queryinput';
  exports.cssText = css;
  /* eslint-enable no-param-reassign */

  const dom = acequire('../lib/dom');
  dom.importCssString(exports.cssText, exports.cssClass);
});
