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
import type { DefaultTheme } from 'styled-components';

import { opacify } from 'theme/sawmill/utils';

import { PALETTES } from './colorPalette';

// Luma design tokens that don't exist in the Graylog theme yet. Values are built from the Luma palette
// so they stay in sync with the Colors page. Used by the Tokens page and by Storybook's own layout.

type ModeValues = { light: string; dark: string };

export const BORDER_TOKENS: Record<'divider' | 'dividerSecondary', ModeValues> = {
  divider: {
    light: opacify(PALETTES.gray.swatches[2].hex, 0.5),
    dark: opacify(PALETTES.gray.swatches[3].hex, 0.5),
  },
  dividerSecondary: {
    light: PALETTES.gray.swatches[2].hex,
    dark: PALETTES.gray.swatches[9].hex,
  },
};

const modeOf = (theme: DefaultTheme) => (theme.mode === 'dark' ? 'dark' : 'light');

/** The `divider` border color for the current light/dark mode. Use for all borders. */
export const dividerColor = (theme: DefaultTheme) => BORDER_TOKENS.divider[modeOf(theme)];

/** A 1px solid border in the `divider` color. */
export const dividerBorder = (theme: DefaultTheme) => `1px solid ${dividerColor(theme)}`;
