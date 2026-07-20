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
  commandTemplate: (host: string, port: number, token: string) => string;
};

const PLATFORMS: Platform[] = [
  {
    id: 'linux',
    label: 'Linux',
    icon: { type: 'brand', name: 'linux' },
    commandTemplate: (host, port, token) =>
      `curl -fsSL https://${host}:${port}/collectors/install | ENROLLMENT_TOKEN=${token} bash`,
  },
  {
    id: 'windows',
    label: 'Windows',
    icon: { type: 'brand', name: 'windows' },
    commandTemplate: (host, port, token) =>
      `Invoke-WebRequest -Uri https://${host}:${port}/collectors/install/windows -OutFile install.ps1; .\\install.ps1 -Token ${token}`,
  },
  {
    id: 'macos',
    label: 'macOS',
    icon: { type: 'brand', name: 'apple' },
    commandTemplate: (host, port, token) =>
      `curl -fsSL https://${host}:${port}/collectors/install | ENROLLMENT_TOKEN=${token} bash`,
  },
];

export default PLATFORMS;
