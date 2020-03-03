// @flow strict
import loadAsync from 'routing/loadAsync';

const AsyncAceEditor = loadAsync(() => import(/* webpackChunkName: "react-ace-builds" */ 'react-ace-builds'));

export default AsyncAceEditor;
