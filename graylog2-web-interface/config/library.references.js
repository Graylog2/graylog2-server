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
