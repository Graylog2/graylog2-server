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
import type { Preview } from '@storybook/react-webpack5';

import { withGraylogTheme } from './withGraylogTheme';
import { lightTheme, darkTheme } from './storybook-theme';
import { DocsContainer } from './DocsContainer';
import { applyStorybookUiStyles } from './storybookUi';

applyStorybookUiStyles();

export const decorators = [withGraylogTheme];

const preview: Preview = {
  parameters: {
    options: {
      // Sidebar order. Storybook reads this function as plain JavaScript: keep it self-contained and without type annotations.
      // 1. Sections: Foundation, Components, Patterns.
      // 2. Foundation pages in a fixed order; everything else alphabetical. "Overview" pages come first in a folder.
      // 3. Within a component, the Docs page is always first, then its stories in file order.
      storySort: (a, b) => {
        const SECTIONS = ['Foundation', 'Components', 'Patterns'];
        const FOUNDATION = ['Typography', 'Spacings', 'Colors', 'Content & Writing'];
        const rank = (list, value) => (list.includes(value) ? list.indexOf(value) : list.length);

        const pathA = a.title.split('/');
        const pathB = b.title.split('/');

        for (let i = 0; i < Math.max(pathA.length, pathB.length); i += 1) {
          const segA = pathA[i];
          const segB = pathB[i];

          if (segA === segB) continue;
          if (segA === undefined) return -1;
          if (segB === undefined) return 1;

          if (i === 0) return rank(SECTIONS, segA) - rank(SECTIONS, segB) || segA.localeCompare(segB);
          if (i === 1 && pathA[0] === 'Foundation') {
            const byRank = rank(FOUNDATION, segA) - rank(FOUNDATION, segB);

            if (byRank !== 0) return byRank;
          }
          if (segA === 'Overview') return -1;
          if (segB === 'Overview') return 1;

          return segA.localeCompare(segB);
        }

        if (a.type === 'docs' && b.type !== 'docs') return -1;
        if (b.type === 'docs' && a.type !== 'docs') return 1;

        return 0;
      },
    },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    darkMode: {
      dark: darkTheme,
      light: lightTheme,
      current: 'dark',
      stylePreview: true,
    },
    docs: {
      container: DocsContainer,
    },
  },
};

export default preview;
