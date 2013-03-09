package controllers;

import models.SearchResult;
import play.mvc.*;

public class Search extends AuthenticatedController {

    public static Result index(String q) {
    	SearchResult searchResult = new SearchResult(q, 9001);
        return ok(views.html.search.results.render(searchResult));
    }

}
