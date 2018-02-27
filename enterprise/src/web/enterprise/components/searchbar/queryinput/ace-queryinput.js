import css from '!raw!./ace-queryinput.css';

// eslint-disable-next-line no-undef
ace.define('ace/theme/ace-queryinput',['require','exports','module','ace/lib/dom'], (acequire, exports) => {
  exports.isDark = false;
  exports.cssClass = 'ace-queryinput';
  exports.cssText = css;

  const dom = acequire('../lib/dom');
  dom.importCssString(exports.cssText, exports.cssClass);
});
