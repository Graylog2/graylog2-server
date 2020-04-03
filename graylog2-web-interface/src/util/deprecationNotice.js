
import AppConfig from './AppConfig';

export const DEPRECATION_NOTICE = 'Graylog Deprecation Notice:';

// eslint-disable-next-line no-console
const deprecationNotice = (deprecatedMessage) => AppConfig.gl2DevMode() && console.warn(DEPRECATION_NOTICE, deprecatedMessage);

export default deprecationNotice;
