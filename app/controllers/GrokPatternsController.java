/*
 * Copyright 2015 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.ExtractorService;
import org.graylog2.restclient.models.GrokPattern;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class GrokPatternsController extends AuthenticatedController {

    private final ExtractorService extractorService;

    @Inject
    public GrokPatternsController(ExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Grok patterns", routes.GrokPatternsController.index());

        final Collection<GrokPattern> grokPatterns;
        try {
            grokPatterns = extractorService.allGrokPatterns();
        } catch (APIException e) {
            return internalServerError();
        } catch (IOException e) {
            return internalServerError();
        }
        
        final TreeSet<GrokPattern> sortedPatterns = Sets.newTreeSet(new Comparator<GrokPattern>() {
            @Override
            public int compare(GrokPattern o1, GrokPattern o2) {
                return ComparisonChain.start()
                        .compare(o1.name, o2.name)
                        .result();
            }
        });
        sortedPatterns.addAll(grokPatterns);
        

        return ok(views.html.system.grokpatterns.index.render(currentUser(), bc, sortedPatterns));
    }
    
    public Result upload() {
        return ok(views.html.system.grokpatterns.index.render(currentUser(), null, null));
        
    }
}
