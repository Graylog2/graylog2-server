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
import { useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import { useErrorsContext } from 'components/lookup-tables/contexts/ErrorsContext';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import ErrorPopover from 'components/lookup-tables/ErrorPopover';
import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';
import Cache from 'components/lookup-tables/Cache';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTableEntity, CachesMap, AdaptersMap } from 'components/lookup-tables/types';

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

const TitleCol = ({ lut, children }: { lut: LookupTableEntity; children: string }) => {
  const { errors } = useErrorsContext();
  const tableErrorText = errors?.lutErrors[lut.name];
  const navigate = useNavigate();

  const onClick = React.useCallback(() => {
    navigate(Routes.SYSTEM.LOOKUPTABLES.show(lut.id));
  }, [navigate, lut.id]);

  return (
    <TitleRow>
      {tableErrorText && <ErrorPopover placement="right" errorText={tableErrorText} title="Lookup Table problem" />}
      <Title onClick={onClick}>{children}</Title>
    </TitleRow>
  );
};

const CacheCol = ({ cacheId, caches }: { cacheId: string; caches: CachesMap }) => {
  const { errors } = useErrorsContext();
  const { setModal, setTitle, setEntity } = useModalContext();

  const onClick = React.useCallback(() => {
    setModal('CACHE');
    setTitle(caches[cacheId].name);
    setEntity(<Cache cache={caches[cacheId]} />);
  }, [cacheId, caches, setModal, setTitle, setEntity]);

  if (!caches || !cacheId || !caches[cacheId]) return <i>No cache</i>;

  const cacheErrorText = errors?.cacheErrors[caches[cacheId].name];

  return (
    <TitleRow>
      {cacheErrorText && <ErrorPopover placement="bottom" errorText={cacheErrorText} title="Cache problem" />}
      <Title onClick={onClick}>{caches[cacheId].title}</Title>
    </TitleRow>
  );
};

const DataAdapterCol = ({ dataAdapterId, dataAdapters }: { dataAdapterId: string; dataAdapters: AdaptersMap }) => {
  const { errors } = useErrorsContext();
  const { setModal, setTitle, setEntity } = useModalContext();

  const onClick = React.useCallback(() => {
    setModal('DATA-ADAPTER');
    setTitle(dataAdapters[dataAdapterId].name);
    setEntity(<DataAdapter dataAdapter={dataAdapters[dataAdapterId]} />);
  }, [dataAdapterId, dataAdapters, setModal, setTitle, setEntity]);

  if (!dataAdapters || !dataAdapterId || !dataAdapters[dataAdapterId]) return <i>No data adapters</i>;

  const adapterErrorText = errors?.adapterErrors[dataAdapters[dataAdapterId].name];

  return (
    <TitleRow>
      {adapterErrorText && (
        <ErrorPopover placement="bottom" errorText={adapterErrorText} title="Data Adapter problem" />
      )}
      <Title onClick={onClick}>{dataAdapters[dataAdapterId].title}</Title>
    </TitleRow>
  );
};

const columnRenderers: ColumnRenderers<LookupTableEntity, { adapters: AdaptersMap; caches: CachesMap }> = {
  attributes: {
    title: {
      width: 0.1,
      renderCell: (title: string, lut) => <TitleCol lut={lut}>{title}</TitleCol>,
    },
    description: {
      width: 0.2,
      renderCell: (description: string) => <span>{description}</span>,
    },
    name: {
      width: 0.1,
      renderCell: (name: string) => <span>{name}</span>,
    },
    cache_id: {
      width: 0.1,
      renderCell: (cache_id: string, _c, meta) => <CacheCol cacheId={cache_id} caches={meta.caches} />,
    },
    data_adapter_id: {
      width: 0.1,
      renderCell: (data_adapter_id: string, _c, meta) => (
        <DataAdapterCol dataAdapterId={data_adapter_id} dataAdapters={meta.adapters} />
      ),
    },
  },
};

export default columnRenderers;
