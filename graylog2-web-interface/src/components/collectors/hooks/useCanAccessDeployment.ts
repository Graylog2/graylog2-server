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
import useCollectorPermissions from './useCollectorPermissions';

/**
 * Whether the Deployment page has anything to offer this user.
 *
 * The page hosts two independently gated tabs — Deploy (needs enrollment token create) and
 * Enrollment tokens (needs enrollment token read) — so it is worth rendering exactly when at
 * least one of them is. Keeping this as the union of the tab gates rather than a separate policy
 * means the page can never render with zero usable tabs.
 *
 * Token delete is deliberately not part of this: the token list is filtered by read permission
 * server-side, so a delete-only user would see an empty list with nothing to act on.
 */
const useCanAccessDeployment = () => {
  const { canDeployCollectors, canViewEnrollmentTokens } = useCollectorPermissions();

  return canDeployCollectors || canViewEnrollmentTokens;
};

export default useCanAccessDeployment;
