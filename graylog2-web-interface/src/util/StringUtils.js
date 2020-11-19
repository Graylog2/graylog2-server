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
  truncateWithEllipses(text = '', maxLength = 10, end = '...') {
    if (text.length > maxLength) {
      return `${text.substring(0, maxLength)}${end}`;
    }

    return text;
  },
};

export default StringUtils;
