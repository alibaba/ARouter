/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.android.arouter.pathvariable.mapping;

import com.alibaba.android.arouter.pathvariable.mapping.util.AntPathMatcher;
import com.alibaba.android.arouter.pathvariable.mapping.util.PathMatcher;
import com.alibaba.android.arouter.pathvariable.mapping.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final Set<String> patterns;

	private final PathMatcher pathMatcher;

	private final boolean useSuffixPatternMatch;

	private final boolean useTrailingSlashMatch;

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is prepended with "/".
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request.
	 */
	public PatternsRequestCondition(String... patterns) {
		this(Arrays.asList(patterns), null, true, true);
	}


	/**
	 * Additional constructor with flags for using suffix pattern (.*) and
	 * trailing slash matches.
	 * @param patterns patterns
	 * @param pathMatcher for path matching with patterns
	 * @param useSuffixPatternMatch whether to enable matching by suffix (".*")
	 * @param useTrailingSlashMatch whether to match irrespective of a trailing slash
	 */
	public PatternsRequestCondition(String[] patterns, PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch) {
		this(Arrays.asList(patterns), pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch);
	}
	/**
	 * Private constructor accepting a collection of patterns.
	 */
	private PatternsRequestCondition(Collection<String> patterns,
			 PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch) {

		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns));
		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}


	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		Set<String> result = new LinkedHashSet<>(patterns.size());
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	public String getFirstPattern() {
		return this.patterns.iterator().next();
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and
	 * the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using {@link PathMatcher#combine(String, String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new PatternsRequestCondition(result, this.pathMatcher, this.useSuffixPatternMatch,
				this.useTrailingSlashMatch);
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance
	 * that is guaranteed to contain matching patterns, sorted via
	 * {@link PathMatcher#getPatternComparator(String)}.
	 * <p>A matching pattern is obtained by making checks in the following order:
	 * <ul>
	 * <li>Direct match
	 * <li>Pattern match with ".*" appended if the pattern doesn't already contain a "."
	 * <li>Pattern match
	 * <li>Pattern match with "/" appended if the pattern doesn't already end in "/"
	 * </ul>
	 * @param lookupPath the request url
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	public PatternsRequestCondition getMatchingCondition(String lookupPath) {
		if (this.patterns.isEmpty()) {
			return this;
		}

		List<String> matches = getMatchingPatterns(lookupPath);

		return matches.isEmpty() ? null :
			new PatternsRequestCondition(matches, this.pathMatcher, this.useSuffixPatternMatch,
					this.useTrailingSlashMatch);
	}

	/**
	 * Find the patterns matching the given lookup path. Invoking this method should
	 * yield results equivalent to those of calling
	 * This method is provided as an alternative to be used if no request is available
	 * (e.g. introspection, tooling, etc).
	 * @param lookupPath the lookup path to match to existing patterns
	 * @return a collection of matching patterns sorted with the closest match at the top
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		List<String> matches = new ArrayList<>();
		for (String pattern : this.patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			if (match != null) {
				matches.add(match);
			}
		}
		Collections.sort(matches, this.pathMatcher.getPatternComparator(lookupPath));
		return matches;
	}

	private String getMatchingPattern(String pattern, String lookupPath) {
		if (pattern.equals(lookupPath)) {
			return pattern;
		}
		if (this.useSuffixPatternMatch) {
			boolean hasSuffix = pattern.indexOf('.') != -1;
			if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
				return pattern + ".*";
			}
		}
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		if (this.useTrailingSlashMatch) {
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				return pattern +"/";
			}
		}
		return null;
	}

	/**
	 * Compare the two conditions based on the URL patterns they contain.
	 * Patterns are compared one at a time, from top to bottom via
	 * {@link PathMatcher#getPatternComparator(String)}. If all compared
	 * patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, String lookupPath) {
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
