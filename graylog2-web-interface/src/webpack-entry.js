import URI from 'urijs';
import AppConfig from 'util/AppConfig';

// If app prefix was not set, we need to tell webpack to load chunks from root instead of the relative URL path
__webpack_public_path__ = URI.joinPaths(AppConfig.gl2AppPathPrefix(), '/').path() || '/';
