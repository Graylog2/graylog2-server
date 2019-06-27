// @flow strict
import loadAsync from 'routing/loadAsync';

const AsyncPlot = loadAsync(() => import('./Plot'));

export default AsyncPlot;
