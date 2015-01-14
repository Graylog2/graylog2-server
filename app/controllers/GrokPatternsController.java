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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.LineReader;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.ExtractorService;
import org.graylog2.restclient.models.GrokPattern;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @BodyParser.Of(BodyParser.MultipartFormData.class)
    public Result upload() {
        String path = getRefererPath();
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart patterns = body.getFile("patterns");
        if (patterns != null) {

            Collection<GrokPattern> grokPatterns = Lists.newArrayList();
            try {
                File file = patterns.getFile();
                String patternsContent = Files.toString(file, StandardCharsets.UTF_8);

                final LineReader lineReader = new LineReader(new StringReader(patternsContent));

                Pattern pattern = Pattern.compile("^([A-z0-9_]+)\\s+(.*)$");
                String line;
                while ((line = lineReader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        final GrokPattern grokPattern = new GrokPattern();
                        grokPattern.name = m.group(1);
                        grokPattern.pattern = m.group(2);
                        grokPatterns.add(grokPattern);
                    }
                }
            } catch (IOException e) {
                Logger.error("Could not parse uploaded file: " + e);
                flash("error", "The uploaded pattern file could not be parsed: does it have the right format?");
                return redirect(path);
            }
            try {
                extractorService.bulkLoadGrokPatterns(grokPatterns);
                flash("success", "Grok patterns added successfully.");
            } catch (APIException | IOException e) {
                flash("error", "There was an error adding the grok patterns, please check the file format.");
            }
        } else {
            flash("error", "You didn't upload any pattern file");
        }
        return redirect(path);
    }
}
