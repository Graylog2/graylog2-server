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
import fs from 'fs';

const writeThemeFile = (fileName: string, theme: unknown) => {
  const fileContent = JSON.stringify(theme, null, 2);

  fs.writeFile(fileName, fileContent, (err) => {
    if (err) {
      // eslint-disable-next-line no-console
      console.error(`Error writing theme ${fileName}:`, err);
    } else {
      // eslint-disable-next-line no-console
      console.log(`Theme "${fileName}" has been created.`);
    }
  });
};

export default writeThemeFile;
