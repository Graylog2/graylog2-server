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
import usePermissions from 'hooks/usePermissions';

export const HEALTH_READ_PERMISSION = 'clusterhealth:read';

/**
 * Returns whether the Health module (panel and nav-bar badge) should be shown. Gated on the dedicated
 * `clusterhealth:read` permission (Decision 6); the backend endpoint enforces the same permission, so the FE and
 * backend agree on visibility. The Enterprise-license check is applied separately by the consumers.
 */
const useHealthModuleVisible = (): boolean => {
  const { isPermitted } = usePermissions();

  return isPermitted(HEALTH_READ_PERMISSION);
};

export default useHealthModuleVisible;
