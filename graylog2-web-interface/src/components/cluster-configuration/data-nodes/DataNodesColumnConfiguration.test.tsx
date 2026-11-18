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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import type { ClusterDataNode } from './fetchClusterDataNodes';
import { createColumnRenderers } from './DataNodesColumnConfiguration';

describe('DataNodesColumnConfiguration', () => {
  const productName = 'Graylog';
  const warningMessage = `This data node version is incompatible with your current ${productName} version, so metrics are disabled.`;

  const renderVersionCell = (versionCompatible: boolean) => {
    const { attributes } = createColumnRenderers(productName);
    const cell = attributes.datanode_version.renderCell(
      undefined,
      {
        datanode_version: '8.0.0',
        version_compatible: versionCompatible,
      } as ClusterDataNode,
      undefined,
    );

    render(<>{cell}</>);
  };

  it('shows warning icon and message for incompatible versions', () => {
    renderVersionCell(false);

    expect(screen.getByText('8.0.0')).toBeInTheDocument();
    expect(screen.getByTitle(warningMessage)).toBeInTheDocument();
  });

  it('omits warning when version is compatible', () => {
    renderVersionCell(true);

    expect(screen.getByText('8.0.0')).toBeInTheDocument();
    expect(screen.queryByTitle(warningMessage)).not.toBeInTheDocument();
  });

  describe('restart flag', () => {
    const renderStatusCell = (pendingRestartNodeIds: Set<string>) => {
      const { attributes } = createColumnRenderers(productName, pendingRestartNodeIds);
      const cell = attributes.datanode_status.renderCell(
        undefined,
        { node_id: 'node-1', datanode_status: 'AVAILABLE' } as ClusterDataNode,
        undefined,
      );

      render(<>{cell}</>);
    };

    it('flags a Data Node which still has to be restarted', () => {
      renderStatusCell(new Set(['node-1']));

      expect(screen.getByText('Restart required')).toBeInTheDocument();
    });

    it('does not flag a Data Node which runs with its saved configuration', () => {
      renderStatusCell(new Set(['another-node']));

      expect(screen.queryByText('Restart required')).not.toBeInTheDocument();
    });
  });
});
