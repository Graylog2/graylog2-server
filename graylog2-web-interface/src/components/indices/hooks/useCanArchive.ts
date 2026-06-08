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
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import { defaultOnError } from 'util/conditional/onError';
import { qualifyUrl } from 'util/URLUtils';

const useCanArchive = (): boolean => {
  const { data: license } = usePluggableLicenseCheck('/license/enterprise/archive');
  const hasArchiveLicense = license.valid;

  const { data: config } = useQuery({
    queryKey: ['archive-config'],
    queryFn: () =>
      defaultOnError(
        fetch('GET', qualifyUrl('/plugins/org.graylog.plugins.archive/config')) as Promise<{
          backend_id: string;
        }>,
        'Loading archive config failed',
        'Could not load archive config',
      ),
    enabled: hasArchiveLicense,
    retry: false,
  });

  return hasArchiveLicense && !!config?.backend_id;
};

export default useCanArchive;
