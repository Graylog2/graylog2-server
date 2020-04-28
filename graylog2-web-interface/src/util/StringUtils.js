const StringUtils = {
  tempDocument: document.createElement('textarea'),
  capitalizeFirstLetter(text) {
    return text.charAt(0).toUpperCase() + text.slice(1);
  },
  escapeHTML(text) {
    this.tempDocument.textContent = text;
    return this.tempDocument.innerHTML;
  },
  unescapeHTML(text) {
    this.tempDocument.innerHTML = text;
    return this.tempDocument.textContent;
  },
  pluralize(number, singular, plural) {
    return (number === 1 || number === '1' ? singular : plural);
  },
  stringify(text) {
    return (typeof text === 'object' ? JSON.stringify(text) : String(text)) || '';
  },
  replaceSpaces(text, newCharacter = '-') {
    return text.replace(/\s/g, newCharacter);
  },
};

export default StringUtils;
