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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

export type Locales = { language_tag: string; display_name: string };

type SystemInfo = {
  cluster_id: string;
  codename: string;
  facility: string;
  hostname: string;
  is_leader: boolean;
  is_processing: boolean;
  lb_status: string;
  lifecycle: string;
  node_id: string;
  operating_system: string;
  started_at: string;
  timezone: string;
  version: string;
};

export const fetchSystemInfo = (): Promise<SystemInfo> =>
  fetch('GET', qualifyUrl(ApiRoutes.SystemApiController.info().url));

export const fetchSystemJvm = () => fetch('GET', qualifyUrl(ApiRoutes.SystemApiController.jvm().url));

export const fetchSystemLocales = (): Promise<{ locales: Array<Locales> }> =>
  fetch('GET', qualifyUrl(ApiRoutes.SystemApiController.locales().url));

const useSystemInfo = () => useQuery({ queryKey: ['system', 'info'], queryFn: fetchSystemInfo });

export const useSystemLocales = () =>
  useQuery({
    queryKey: ['system', 'locales'],
    queryFn: fetchSystemLocales,
    select: (data) => data.locales,
  });

export { useSystemInfo };
export default useSystemInfo;
