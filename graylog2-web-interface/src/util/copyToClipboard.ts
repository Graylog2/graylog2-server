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

// Simple fallback if the clipboard API does not exist/is inaccessible
const legacyWriteText = (str: string) => {
  const listener = (e: ClipboardEvent) => {
    e.clipboardData.setData('text/plain', str);
    e.preventDefault();
  };

  document.addEventListener('copy', listener);
  document.execCommand('copy');
  document.removeEventListener('copy', listener);

  return Promise.resolve();
};

// Compatibility is sufficient. We not support IE or outdated iOS.

const copyToClipboard = (text: string) => (navigator.clipboard && window.isSecureContext

  ? navigator.clipboard.writeText(text)
  : legacyWriteText(text));

export default copyToClipboard;
