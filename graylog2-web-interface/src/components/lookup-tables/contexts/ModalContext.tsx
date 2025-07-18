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

const ModalContext = React.createContext(null);

export function ModalProvider<ModalTypes, EntityType = unknown>({ children }: { children: React.ReactNode }) {
  const [modal, setModal] = React.useState<ModalTypes>(null);
  const [entity, setEntity] = React.useState<EntityType>();
  const [title, setTitle] = React.useState<string>(null);
  const [double, setDouble] = React.useState<boolean>(false);
  const value = React.useMemo(
    () => ({ modal, setModal, entity, setEntity, title, setTitle, double, setDouble }),
    [modal, entity, title, double],
  );

  return <ModalContext.Provider value={value}>{children}</ModalContext.Provider>;
}

export function useModalContext() {
  const context = React.useContext(ModalContext);

  if (!context) {
    throw new Error('useModalContext must be used within a ModalProvider');
  }

  return context;
}
