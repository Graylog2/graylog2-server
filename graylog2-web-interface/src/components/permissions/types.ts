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

import type React from 'react';

import type { Attribute } from 'stores/PaginationTypes';

import type { ColumnRenderer, EntityBase } from '../common/EntityDataTable/types';

export type ModalHandler = {
  toggle?: () => void;
  onConfirm?: () => void;
  onCancel?: () => void;
};

export type EntityActionModalProps<T, M> = React.PropsWithRef<{
  entity: T;
  entityType: string;
}> & {
  ref: React.LegacyRef<M>;
};


export type EntityActionComponentProps<T, M> = {
  entity: T;
  modalRef: () => M;
};

export type EntitySharedAction<T, M> = {
  key: string;
  component: React.ComponentType<EntityActionComponentProps<T, M>>;
  modal?: React.ComponentType<EntityActionModalProps<T, M>>;
  useCondition?: () => boolean;
};
export type TableElement<T extends EntityBase> = {
  attributeName: string;
  attributes: Array<Attribute>;
  getColumnRenderer: (entityType: string) => ColumnRenderer<T, unknown>;
  expandedSection: any;
  useCondition: () => true,
}

declare module 'graylog-web-plugin/plugin' {

  export interface PluginExports {
    'components.shared.entityActions'?: Array<EntitySharedAction<unknown, unknown>>;
    'components.shared.entityTableElements'?: Array<TableElement<EntityBase>>;
  }
}
