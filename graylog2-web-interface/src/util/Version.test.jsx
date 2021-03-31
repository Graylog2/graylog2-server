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

// eslint-disable-next-line import/no-named-as-default
import Version, { getFullVersion, getMajorAndMinorVersion, parseVersion } from './Version';

jest.unmock('util/Version');
jest.mock('../../package.json', () => ({ version: '4.0.6-SNAPSHOT' }));

describe('<Version>', () => {
  it('returns the full version from package information', () => {
    const version = '4.0.6-SNAPSHOT';

    // eslint-disable-next-line import/no-named-as-default-member
    expect(Version.getFullVersion()).toBe(version);
    expect(getFullVersion()).toBe(version);
  });

  it('returns the major and minor version from package information', () => {
    // eslint-disable-next-line import/no-named-as-default-member
    expect(Version.getMajorAndMinorVersion()).toBe('4.0');
    expect(getMajorAndMinorVersion()).toBe('4.0');
  });

  it('parses version and returns an object with its components', () => {
    const versions = {
      final: {
        version: '4.0.6',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
        },
      },
      finalWithBuild: {
        version: '4.0.6+35bdc1',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
          buildMetadata: '35bdc1',
        },
      },
      snapshotVersion: {
        version: '4.0.6-SNAPSHOT',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
          preRelease: 'SNAPSHOT',
        },
      },
      snapshotWithBuildVersion: {
        version: '4.0.6-SNAPSHOT+35bdc1',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
          preRelease: 'SNAPSHOT',
          buildMetadata: '35bdc1',
        },
      },
      betaVersion: {
        version: '4.0.6-beta.1',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
          preRelease: 'beta.1',
        },
      },
      betaWithBuildVersion: {
        version: '4.0.6-beta.1+35bdc1',
        expected: {
          major: '4',
          minor: '0',
          patch: '6',
          preRelease: 'beta.1',
          buildMetadata: '35bdc1',
        },
      },
    };

    Object.keys(versions).forEach((versionType) => {
      const { version, expected } = versions[versionType];

      expect(parseVersion(version)).toEqual(expected);
    });
  });
});
