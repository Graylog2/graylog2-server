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
import styled from 'styled-components';

import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';

import ElementConfigurationSection from './ElementConfigurationSection';
import ElementsConfigurationActions from './ElementsConfigurationActions';
import type { AggregationElement } from './AggregationElementType';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const Container = styled.div`
  position: relative;
  height: 100%;
`;

type Props = {
  aggregationElementsByKey: { [k: string]: AggregationElement<keyof WidgetConfigFormValues> },
  onCreate: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
  onSubmit: () => void,
  onCancel: () => void,
}

const ElementsConfiguration = ({ aggregationElementsByKey, onCreate, onSubmit, onCancel }: Props) => (
  <Container>
    <StickyBottomActions actions={(
      <>
        <ElementsConfigurationActions />
        <SaveOrCancelButtons onCancel={onCancel} onSubmit={onSubmit} />
      </>
    )}>
      <div>
        {Object.values(aggregationElementsByKey).map((aggregationElement) => {
          const { component: ConfigurationSection } = aggregationElement;

          return (
            <ElementConfigurationSection aggregationElement={aggregationElement}
                                         onCreate={onCreate}
                                         key={aggregationElement.key}>
              <ConfigurationSection />
            </ElementConfigurationSection>
          );
        })}
      </div>
    </StickyBottomActions>
  </Container>
);

export default ElementsConfiguration;
