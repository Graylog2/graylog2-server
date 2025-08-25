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
import styled, { css } from 'styled-components';

import { SegmentedControl } from 'components/bootstrap';
import { Section } from 'components/common';
import type { RetentionStrategyContext, Strategies } from 'components/indices/Types';
import type { IndexSetFormValues, IndexSet } from 'stores/indices/IndexSetsStore';
import IndexSetRotationRetentionLegacyConfiguration from 'components/indices/IndexSetConfigurationForm/IndexSetRotationRetentionLegacyConfiguration';
import IndexSetRotationRetentionDataTieringConfiguration from 'components/indices/IndexSetConfigurationForm/IndexSetRotationRetentionDataTieringConfiguration';

type RetentionConfigSegment = 'data_tiering' | 'legacy';

type Props = {
  values: IndexSetFormValues;
  indexSet: IndexSet;
  retentionStrategies: Strategies;
  retentionStrategiesContext: RetentionStrategyContext;
  rotationStrategies: Strategies;
  hiddenFields: string[];
  immutableFields: string[];
  hasFieldRestrictionPermission: boolean;
  isCloud: boolean;
  enableDataTieringCloud: boolean;
  selectedRetentionSegment: RetentionConfigSegment;
  setSelectedRetentionSegment: React.Dispatch<React.SetStateAction<RetentionConfigSegment>>;
};

const ConfigSegment = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
  `,
);

const IndexSetRotationRetentionConfigurationSection = ({
  values,
  indexSet,
  rotationStrategies,
  retentionStrategies,
  retentionStrategiesContext,
  hiddenFields,
  immutableFields,
  hasFieldRestrictionPermission,
  isCloud,
  enableDataTieringCloud,
  selectedRetentionSegment,
  setSelectedRetentionSegment,
}: Props) => {
  const retentionConfigSegments: Array<{ value: RetentionConfigSegment; label: string }> = [
    { value: 'data_tiering', label: 'Data Tiering' },
    { value: 'legacy', label: 'Legacy (Deprecated)' },
  ];

  const legacyRenderable = (): boolean => {
    if (hiddenFields.includes('legacy') && !hasFieldRestrictionPermission) return false;

    if (
      hiddenFields.includes('legacy.rotation_strategy') &&
      hiddenFields.includes('legacy.retention_strategy') &&
      !hasFieldRestrictionPermission
    )
      return false;

    return true;
  };

  const dataTieringRenderable = (): boolean => {
    if (hiddenFields.includes('data_tiering') && !hasFieldRestrictionPermission) return false;

    if (isCloud && !enableDataTieringCloud) return false;

    return true;
  };

  if (!dataTieringRenderable() && !legacyRenderable()) return null;

  if (!dataTieringRenderable())
    return (
      <Section title="Rotation & Retention">
        <IndexSetRotationRetentionLegacyConfiguration
          indexSet={indexSet}
          values={values}
          retentionStrategies={retentionStrategies}
          rotationStrategies={rotationStrategies}
          retentionStrategiesContext={retentionStrategiesContext}
          hiddenFields={hiddenFields}
          immutableFields={immutableFields}
          hasFieldRestrictionPermission={hasFieldRestrictionPermission}
        />
      </Section>
    );

  if (!legacyRenderable())
    return (
      <Section title="Rotation & Retention">
        <IndexSetRotationRetentionDataTieringConfiguration
          values={values}
          hiddenFields={hiddenFields}
          immutableFields={immutableFields}
          hasFieldRestrictionPermission={hasFieldRestrictionPermission}
        />
      </Section>
    );

  return (
    <Section title="Rotation & Retention">
      <>
        <SegmentedControl<RetentionConfigSegment>
          data={retentionConfigSegments}
          value={selectedRetentionSegment}
          onChange={setSelectedRetentionSegment}
        />
        {selectedRetentionSegment === 'data_tiering' ? (
          <IndexSetRotationRetentionDataTieringConfiguration
            values={values}
            hiddenFields={hiddenFields}
            immutableFields={immutableFields}
            hasFieldRestrictionPermission={hasFieldRestrictionPermission}
          />
        ) : (
          <ConfigSegment>
            <IndexSetRotationRetentionLegacyConfiguration
              indexSet={indexSet}
              values={values}
              retentionStrategies={retentionStrategies}
              rotationStrategies={rotationStrategies}
              retentionStrategiesContext={retentionStrategiesContext}
              hiddenFields={hiddenFields}
              immutableFields={immutableFields}
              hasFieldRestrictionPermission={hasFieldRestrictionPermission}
            />
          </ConfigSegment>
        )}
      </>
    </Section>
  );
};
export default IndexSetRotationRetentionConfigurationSection;
