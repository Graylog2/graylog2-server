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
import { paletteHex } from '../stories/Foundation/colorPalette';

// Styles for Storybook's own UI (sidebar and toolbar):
// - icons use the Luma icon color: Gray 5 in light mode, White in dark mode (from the Luma palette data)
// - the Graylog logo in the sidebar header is 38px tall
//
// Storybook's UI (the "manager") can't use the Graylog theme, so this runs from the preview
// and adds a small stylesheet to the surrounding Storybook page. It updates without a restart.

/** Luma icon color for Storybook's UI: Gray 5 in light mode, White in dark mode. */
export const UI_ICON_COLORS = {
  light: paletteHex('Gray', '5'),
  dark: paletteHex(undefined, 'White'),
};

const LIGHT_ICON = UI_ICON_COLORS.light;
const DARK_ICON = UI_ICON_COLORS.dark;

const STYLE_ID = 'luma-storybook-ui';

const css = `
  .sidebar-container svg,
  [data-testid='sb-preview-toolbar'] svg {
    color: ${LIGHT_ICON} !important;
  }

  body.dark .sidebar-container svg,
  body.dark [data-testid='sb-preview-toolbar'] svg {
    color: ${DARK_ICON} !important;
  }

  /* The selected sidebar item keeps its own icon color so it stays readable on the highlight. */
  .sidebar-container [data-selected='true'] svg {
    color: inherit !important;
  }

  /* Graylog logo in the sidebar header. */
  .sidebar-container img[alt='Graylog Design System'] {
    height: 38px;
    width: auto;
    max-height: none;
  }
`;

export const applyStorybookUiStyles = () => {
  try {
    const managerDocument = window.parent?.document;

    if (!managerDocument || managerDocument === document) return;

    let style = managerDocument.getElementById(STYLE_ID);

    if (!style) {
      style = managerDocument.createElement('style');
      style.id = STYLE_ID;
      managerDocument.head.appendChild(style);
    }

    style.textContent = css;
  } catch {
    // The Storybook UI isn't reachable (e.g. the preview is opened on its own). Nothing to style.
  }
};
