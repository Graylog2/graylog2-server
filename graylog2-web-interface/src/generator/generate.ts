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
