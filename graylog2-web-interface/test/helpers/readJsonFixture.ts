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
import { readFileSync } from 'fs';

/**
 * This simple helper function allows reading a fixture file to be able to use its content in a test.
 *
 * @param {fixtureDir} - path of the fixture directory.
 * Usually its value is `__dirname`, because the fixture is in the same directory as the test.
 *
 * @param {fixturePath} - name of the fixture file.
 */
const readJsonFixture = (fixtureDir: string, fixtureName: string) => (
  JSON.parse(readFileSync(`${fixtureDir}/${fixtureName}`).toString('utf8'))
);

export default readJsonFixture;
