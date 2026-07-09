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
import '@material-symbols/font-700/rounded.css';
import materialSymbolsFont from '@material-symbols/font-700/material-symbols-rounded.woff2';

// The icon `@font-face` uses `font-display: block`, so the browser only fetches the font
// lazily when the first icon paints. In slow/cold environments (e.g. headless e2e) that
// fetch can miss the block window, causing icons to fall back to their ligature source
// text ("search", ...). Emitting a real `<link rel="preload">` from this early-loading
// entry (it runs before `app.js`) starts the fetch up front, so the font is ready by the
// time icons first render. `crossorigin` is required for the preload to be reused by the
// CORS fetch that `@font-face` performs.
const preloadIconFont = () => {
  const link = document.createElement('link');
  link.rel = 'preload';
  link.as = 'font';
  link.type = 'font/woff2';
  link.href = materialSymbolsFont;
  link.crossOrigin = 'anonymous';
  document.head.appendChild(link);
};

preloadIconFont();
