import Promise from 'bluebird';
import Reflux from 'reflux';

Reflux.setPromiseFactory(handlers => new Promise(handlers));
