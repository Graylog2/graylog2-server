import URI from 'urijs';
import AppConfig from 'util/AppConfig';

__webpack_public_path__ = URI.joinPaths(AppConfig.gl2AppPathPrefix(), '/').path();
