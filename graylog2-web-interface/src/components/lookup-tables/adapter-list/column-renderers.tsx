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

import { MetricContainer, CounterRate } from 'components/metrics';
import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import ErrorPopover from 'components/lookup-tables/ErrorPopover';
import { useErrorsContext } from 'components/lookup-tables/contexts/ErrorsContext';
import type { DataAdapterEntity } from 'components/lookup-tables/types';

const TitleRow = styled.div`
  display: flex;
  flex-direction: row;
  align-items: flex-end;
`;

const Title = styled.div`
  color: ${({ theme }) => theme.colors.link.default};
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.link.hover};
    text-decoration: underline;
  }
`;

const TitleCol = ({ adapter, children }: { adapter: DataAdapterEntity; children: string }) => {
  const { errors } = useErrorsContext();
  const adapterErrorText = errors?.adapterErrors[adapter.name];
  const { setModal, setTitle, setEntity } = useModalContext();

  const onClick = React.useCallback(() => {
    setModal('DATA-ADAPTER');
    setTitle(adapter.name);
    setEntity(<DataAdapter dataAdapter={adapter} />);
  }, [adapter, setModal, setTitle, setEntity]);

  return (
    <TitleRow>
      {adapterErrorText && <ErrorPopover placement="right" errorText={adapterErrorText} title="Data Adapter problem" />}
      <Title onClick={onClick}>{children}</Title>
    </TitleRow>
  );
};

const ThroughputCol = ({ adapter }: { adapter: DataAdapterEntity }) => (
  <MetricContainer name={`org.graylog2.lookup.adapters.${adapter.id}.requests`}>
    <CounterRate suffix="lookups/s" />
  </MetricContainer>
);

const columnRenderers: ColumnRenderers<DataAdapterEntity> = {
  attributes: {
    title: {
      width: 0.2,
      renderCell: (title: string, adapter: DataAdapterEntity) => <TitleCol adapter={adapter}>{title}</TitleCol>,
    },
    description: {
      width: 0.2,
      renderCell: (description: string) => <span>{description}</span>,
    },
    name: {
      width: 0.2,
      renderCell: (name: string) => <span>{name}</span>,
    },
    throughput: {
      staticWidth: 150,
      renderCell: (_arg: unknown, adapter: DataAdapterEntity) => <ThroughputCol adapter={adapter} />,
    },
  },
};

export default columnRenderers;
