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

import type { IconName } from 'components/common/Icon';
import type BrandIcon from 'components/common/BrandIcon';

export type PlatformId = 'linux' | 'windows' | 'macos' | 'kubernetes' | 'docker';

type BrandIconRef = { type: 'brand'; name: React.ComponentProps<typeof BrandIcon>['name'] };
type MaterialIconRef = { type: 'material'; name: IconName };
export type PlatformIcon = BrandIconRef | MaterialIconRef;

type Platform = {
  id: PlatformId;
  label: string;
  icon: PlatformIcon;
  /** `endpoint` is the full enroll URL — see `enrollEndpointUrl` in `collectors/common`. */
  commandTemplate: (endpoint: string, token: string) => string;
};

/** Install scripts published from the collector repo (`dist/install/`). */
const INSTALL_SCRIPT_BASE_URL = 'https://downloads.graylog.org/repo/scripts/collector';

const shellInstallCommand = (script: string) => (endpoint: string, token: string) =>
  `curl -fsSL ${INSTALL_SCRIPT_BASE_URL}/${script} | sudo sh -s -- --endpoint ${endpoint} --token ${token}`;

const PLATFORMS: Platform[] = [
  {
    id: 'linux',
    label: 'Linux',
    icon: { type: 'brand', name: 'linux' },
    commandTemplate: shellInstallCommand('install-linux.sh'),
  },
  {
    id: 'windows',
    label: 'Windows',
    icon: { type: 'brand', name: 'windows' },
    // Running the script as a scriptblock passes the parameters through and sidesteps the
    // execution policy, which would block a downloaded `.ps1` file by default.
    commandTemplate: (endpoint, token) =>
      `& ([scriptblock]::Create((irm ${INSTALL_SCRIPT_BASE_URL}/install-windows.ps1))) -Endpoint ${endpoint} -Token ${token}`,
  },
  {
    id: 'macos',
    label: 'macOS',
    icon: { type: 'brand', name: 'apple' },
    commandTemplate: shellInstallCommand('install-macos.sh'),
  },
];

export default PLATFORMS;
