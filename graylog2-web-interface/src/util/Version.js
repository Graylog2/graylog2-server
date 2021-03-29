// @flow strict
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
import pjson from '../../package.json';

const versionRegex = /(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(-(?<preRelease>[\w.\d]+))?(\+(?<buildMetadata>\w+))?/;
const defaultVersion = pjson.version;

export type Version = {
  major: string,
  minor: string,
  patch: string,
  preRelease?: string,
  buildMetadata?: string,
};

export const parseVersion = (version?: string = defaultVersion): Version | void => {
  const result = versionRegex.exec(version);

  if (!result || !result.groups) {
    console.error('Failed to parse version', version);

    return undefined;
  }

  const versionGroups = (result.groups: Version);

  return {
    major: versionGroups.major,
    minor: versionGroups.minor,
    patch: versionGroups.patch,
    preRelease: versionGroups.preRelease,
    buildMetadata: versionGroups.buildMetadata,
  };
};

export const getFullVersion = (): string => {
  return defaultVersion;
};

export const getMajorAndMinorVersion = (): string => {
  const result = parseVersion();

  if (!result) {
    return getFullVersion();
  }

  const { major, minor } = result;

  return `${major}.${minor}`;
};

export default {
  parseVersion,
  getMajorAndMinorVersion,
  getFullVersion,
};
