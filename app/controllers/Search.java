package controllers;

import java.io.IOException;

import lib.APIException;
import models.SearchResult;
import models.UniversalSearch;
import play.mvc.*;

public class Search extends AuthenticatedController {

    public static Result index(String q) {
		try {
			UniversalSearch search = new UniversalSearch(q);
			SearchResult searchResult = search.execute();
			return ok(views.html.search.results.render(searchResult));
		} catch (IOException e) {
			return ok("io exception");
		} catch (APIException e) {
			return ok("api exception" + e);
		}
    }

}
