package controllers;

import play.mvc.Result;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamAlertsController extends AuthenticatedController {
    public Result index(String streamId) {
        return ok();
    }
}
