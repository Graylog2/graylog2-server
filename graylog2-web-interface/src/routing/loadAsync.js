import loadable, { setConfig } from 'loadable-components';

setConfig({ hotReload: false });

export default f => loadable(() => f().then(c => c.default));
