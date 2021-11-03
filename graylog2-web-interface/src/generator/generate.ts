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
#!/usr/bin/env ts-node-script
import fs from 'fs';
import { dirname } from 'path';

import emit from './emit';
import parse from './parse';

if (process.argv.length !== 4) {
  console.error('Syntax: generate <source dir> <destination dir>');
  process.exit(-1);
}

const [srcDir, dstDir] = process.argv.slice(2);

const writeFile = (filename: string, content: string) => {
  const fullFilename = `${dstDir}/${filename}.ts`;
  console.log(`Writing ${fullFilename} ...`);
  const directory = dirname(fullFilename);
  fs.mkdirSync(directory, { recursive: true });

  fs.writeFileSync(fullFilename, content);
};

const apis = parse(srcDir);
const [apiFiles, summary] = emit(apis);

Object.entries(apiFiles)
  .forEach(([filename, content]) => writeFile(filename, content));

writeFile('index', summary);
