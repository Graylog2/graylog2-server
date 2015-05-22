package lib.sockjs;

import play.libs.F;
import play.sockjs.SockJS;

/**
 * A very unfortunate copy of the play-sockjs router class, because we need to be able to override the websocket true/false
 * parameter, but the constructor was private.
 */

public abstract class SockJsRouter extends play.sockjs.core.j.JavaRouter {

    public SockJsRouter(F.Option<SockJS.Settings> cfg) {
        super(cfg);
    }

}
