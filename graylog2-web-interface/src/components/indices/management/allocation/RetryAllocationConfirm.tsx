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
import * as React from 'react';

import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

type Props = {
  isRetrying: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

const RetryAllocationConfirm = ({ isRetrying, onConfirm, onCancel }: Props) => (
  <BootstrapModalConfirm
    showModal
    title="Retry failed shard allocations?"
    confirmButtonText="Retry"
    isAsyncSubmit
    isSubmitting={isRetrying}
    submitLoadingText="Retrying..."
    onConfirm={onConfirm}
    onCancel={onCancel}>
    <>
      <p>
        After a shard fails to allocate several times in a row (<code>index.allocation.max_retries</code>, 5 by
        default), OpenSearch stops trying. This asks it to try those shards again (
        <code>_cluster/reroute?retry_failed=true</code>).
      </p>
      <p>
        It moves no data and doesn&apos;t force stale or empty primaries, so nothing is lost. It fixes temporary causes
        such as a node that was down or a full disk. If the cause is still there (corrupted files, for example), the
        shards fail again.
      </p>
    </>
  </BootstrapModalConfirm>
);

export default RetryAllocationConfirm;
