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
import { useTree } from '@mantine/core';

import { Col, Row } from 'components/bootstrap';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import HealthDetailsPane from './HealthDetailsPane';
import HealthInterpretationLegend from './HealthInterpretationLegend';
import HealthTreePane from './HealthTreePane';
import useHealthModule from './useHealthModule';
import useHealthModuleVisible from './useHealthModuleVisible';
import { ModuleContent, ModuleLayout } from './HealthModule.styles';

const HealthModule = () => {
  const showHealthModule = useHealthModuleVisible();
  const { data: { valid: hasEnterpriseLicense } = { valid: false } } = usePluggableLicenseCheck('/license/enterprise');
  const { initialExpandedState, lookup, paths, root, treeData } = useHealthModule();
  const tree = useTree({
    initialExpandedState,
    initialSelectedState: [root.id],
  });

  if (!showHealthModule || !hasEnterpriseLicense) return null;

  const selectedValue = tree.selectedState[0] ?? root.id;
  const selectedNode = lookup[selectedValue] ?? root;
  const selectedPath = paths[selectedNode.id] ?? [];
  const isRootSelected = selectedNode.id === root.id;

  return (
    <Row className="content">
      <Col md={12}>
        <h2>Health of Graylog Deployment</h2>

        <ModuleContent>
          <ModuleLayout>
            <HealthTreePane tree={tree} treeData={treeData} lookup={lookup} root={root} />

            {isRootSelected ? (
              <HealthInterpretationLegend />
            ) : (
              <HealthDetailsPane tree={tree} selectedNode={selectedNode} selectedPath={selectedPath} />
            )}
          </ModuleLayout>
        </ModuleContent>
      </Col>
    </Row>
  );
};

export default HealthModule;
