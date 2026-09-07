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
import { createContext, useContext } from 'react';

export type PngExportFn = () => Promise<string>;

type PngExportContextType = {
  exportFn: PngExportFn | null;
  setExportFn: (fn: PngExportFn | null) => void;
  widgetTitle: string;
};

const defaultValue: PngExportContextType = {
  exportFn: null,
  setExportFn: () => {},
  widgetTitle: '',
};

// The context object must be shared across webpack chunks (core + plugins).
// Storing it on window ensures all chunks use the same instance even when
// each chunk bundles its own copy of this module.
const CONTEXT_KEY = '__gl_png_export_ctx__';
type WindowWithCtx = typeof window & { [CONTEXT_KEY]?: React.Context<PngExportContextType> };

const win = window as WindowWithCtx;
if (!win[CONTEXT_KEY]) {
  win[CONTEXT_KEY] = createContext<PngExportContextType>(defaultValue);
}

const PngExportContext = win[CONTEXT_KEY] as React.Context<PngExportContextType>;

export const usePngExportContext = () => useContext(PngExportContext);

export default PngExportContext;
