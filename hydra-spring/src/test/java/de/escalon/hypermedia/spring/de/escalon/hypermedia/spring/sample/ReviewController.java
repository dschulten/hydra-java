/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.sample;

import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;

/**
 * Sample controller for reviews.
 * Created by dschulten on 16.09.2014.
 */
@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @SuppressWarnings("unchecked")
    List<List<Review>> reviews = Arrays.asList(Arrays.asList(new Review("Five peeps, one guitar")),
            Arrays.asList(new Review("Great actress, special atmosphere")));

    @RequestMapping("/events/{eventId}")
    @ResponseBody
    public Resources<Review> getReviews(@PathVariable int eventId) {
        return new Resources<Review>(reviews.get(eventId));
    }
}
