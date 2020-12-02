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
const webpack = require('webpack');
const merge = require('webpack-merge');

const BUILD_PATH = path.resolve(__dirname);
const VENDOR_MANIFEST_PATH = path.resolve(BUILD_PATH, 'vendor-manifest.json');
const VENDOR_MANIFEST = require(VENDOR_MANIFEST_PATH);
const SHARED_MANIFEST_PATH = path.resolve(BUILD_PATH, 'shared-manifest.json');
const SHARED_MANIFEST = require(SHARED_MANIFEST_PATH);

const libraryReference = function(manifest, root_path) {
  return {
    plugins: [
      new webpack.DllReferencePlugin({manifest: manifest, context: root_path}),
    ],
  };
};

export default function(root_path) {
  return merge.smart(
    libraryReference(VENDOR_MANIFEST, root_path),
    libraryReference(SHARED_MANIFEST, root_path)
  );
};
