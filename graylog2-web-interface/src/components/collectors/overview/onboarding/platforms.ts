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

export type PlatformId = 'linux' | 'windows' | 'macos' | 'kubernetes' | 'docker';

type BrandIconRef = { type: 'brand'; name: 'apple' | 'linux' | 'windows' };
type MaterialIconRef = { type: 'material'; name: IconName };
export type PlatformIcon = BrandIconRef | MaterialIconRef;

export type Platform = {
  id: PlatformId;
  label: string;
  icon: PlatformIcon;
  commandTemplate: (host: string, port: number, token: string) => string;
  sourceTypes: string[];
};

const PLATFORMS: Platform[] = [
  {
    id: 'linux',
    label: 'Linux',
    icon: { type: 'brand', name: 'linux' },
    commandTemplate: (host, port, token) =>
      `curl -fsSL https://${host}:${port}/collectors/install | ENROLLMENT_TOKEN=${token} bash`,
    sourceTypes: ['syslog', 'auth.log', 'journald'],
  },
  {
    id: 'windows',
    label: 'Windows',
    icon: { type: 'brand', name: 'windows' },
    commandTemplate: (host, port, token) =>
      `Invoke-WebRequest -Uri https://${host}:${port}/collectors/install/windows -OutFile install.ps1; .\\install.ps1 -Token ${token}`,
    sourceTypes: ['Windows Event Log', 'IIS'],
  },
  {
    id: 'macos',
    label: 'macOS',
    icon: { type: 'brand', name: 'apple' },
    commandTemplate: (host, port, token) =>
      `curl -fsSL https://${host}:${port}/collectors/install | ENROLLMENT_TOKEN=${token} bash`,
    sourceTypes: ['syslog', 'unified log'],
  },
  {
    id: 'kubernetes',
    label: 'Kubernetes',
    icon: { type: 'material', name: 'cloud' },
    commandTemplate: (host, port, token) =>
      `helm install graylog-collector oci://${host}:${port}/collectors/charts/collector --set enrollmentToken=${token}`,
    sourceTypes: ['container logs', 'pod metadata'],
  },
  {
    id: 'docker',
    label: 'Docker',
    icon: { type: 'material', name: 'deployed_code' },
    commandTemplate: (host, port, token) =>
      `docker run -d -e ENROLLMENT_TOKEN=${token} ${host}:${port}/collectors/collector:latest`,
    sourceTypes: ['container stdout', 'container stderr'],
  },
];

export default PLATFORMS;
