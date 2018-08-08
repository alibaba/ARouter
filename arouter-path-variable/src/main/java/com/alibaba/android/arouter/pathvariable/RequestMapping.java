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

package com.alibaba.android.arouter.pathvariable;
import java.util.*;
import com.alibaba.android.arouter.pathvariable.util.AntPathMatcher;
import com.alibaba.android.arouter.pathvariable.util.Assert;
import com.alibaba.android.arouter.pathvariable.util.LinkedMultiValueMap;
import com.alibaba.android.arouter.pathvariable.util.MultiValueMap;
import com.alibaba.android.arouter.pathvariable.util.PathMatcher;

/**
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 * needed to match the handler method to incoming request.
 */
public class RequestMapping {
	private final MappingRegistry mappingRegistry = new MappingRegistry();

	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Return the internal mapping registry. Provided for testing purposes.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * Register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping for the handler method
	 */
	public void registerMapping(RequestMappingInfo mapping) {
		this.mappingRegistry.register(mapping);
	}

	/**
	 * Un-register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(RequestMappingInfo mapping) {
		this.mappingRegistry.unregister(mapping);
	}


	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @return the best-matching handler method, or {@code null} if no match
	 * @throws Exception exception
	 */
	public RequestMappingInfo lookupHandlerMethod(String lookupPath) throws Exception {
		List<Match> matches = new ArrayList<>();
		List<RequestMappingInfo> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
		if (directPathMatches != null) {
			addMatchingMappings(directPathMatches, matches, lookupPath);
		}
		if (matches.isEmpty()) {
			// No choice but to go through all mappings...
			addMatchingMappings(this.mappingRegistry.getMappings(), matches, lookupPath);
		}

		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(lookupPath));
			Collections.sort(matches, comparator);
			//System.out.println("Found " + matches.size() + " matching mapping(s) for [" + lookupPath + "] : " + matches);
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				Match secondBestMatch = matches.get(1);
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					RequestMappingInfo m1 = bestMatch.getMapping();
					RequestMappingInfo m2 = secondBestMatch.getMapping();
					throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" + lookupPath + "': {" + m1 + ", " + m2 + "}");
				}
			}
			handleMatch(bestMatch.mapping, lookupPath);
			return bestMatch.getMapping();
		}
		else {
			return handleNoMatch(this.mappingRegistry.getMappings(), lookupPath);
		}
	}

	private RequestMappingInfo handleNoMatch(Set<RequestMappingInfo> ts, String lookupPath) {
		return null;
	}

	private void handleMatch(RequestMappingInfo mapping, String lookupPath) {

	}

	/**
	 * Return the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	private void addMatchingMappings(Collection<RequestMappingInfo> mappings, List<Match> matches, String lookupPath) {
		for (RequestMappingInfo mapping : mappings) {
			RequestMappingInfo match = getMatchingMapping(mapping, lookupPath);
			if (match != null) {
				matches.add(new Match(match));
			}
		}
	}

	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, String lookupPath) {
		return info.getMatchingCondition(lookupPath);
	}

	protected Comparator<RequestMappingInfo> getMappingComparator(final String lookupPath) {
		return new Comparator<RequestMappingInfo>() {
			@Override
			public int compare(RequestMappingInfo info1, RequestMappingInfo info2) {
				return info1.compareTo(info2, lookupPath);
			}
		};
	}

	public void cleanup() {
		mappingRegistry.cleanup();
	}

	/**
	 * A registry that maintains all mappings to handler methods, exposing methods
	 * to perform lookups and providing concurrent access.
	 *
	 * <p>Package-private for testing purposes.
	 */
	class MappingRegistry {
		private final Map<RequestMappingInfo, MappingRegistration<RequestMappingInfo>> registry = new HashMap<>();

		private final Set<RequestMappingInfo> mappingLookup = new LinkedHashSet<>();

		private final MultiValueMap<String, RequestMappingInfo> urlLookup = new LinkedMultiValueMap<>();

		/**
		 * Return all mappings and handler methods. Not thread-safe.
		 */
		public Set<RequestMappingInfo> getMappings() {
			return this.mappingLookup;
		}

		/**
		 * Return matches for the given URL path. Not thread-safe.
		 */
		public List<RequestMappingInfo> getMappingsByUrl(String urlPath) {
			return this.urlLookup.get(urlPath);
		}


		public void register(RequestMappingInfo mapping) {
			this.mappingLookup.add(mapping);

			List<String> directUrls = getDirectUrls(mapping);
			for (String url : directUrls) {
				this.urlLookup.add(url, mapping);
			}
			this.registry.put(mapping, new MappingRegistration<>(mapping, directUrls));
		}

		private List<String> getDirectUrls(RequestMappingInfo mapping) {
			List<String> urls = new ArrayList<>(1);
			for (String path : getMappingPathPatterns(mapping)) {
				if (!getPathMatcher().isPattern(path)) {
					urls.add(path);
				}
			}
			return urls;
		}

		public void unregister(RequestMappingInfo mapping) {
			MappingRegistration<RequestMappingInfo> definition = this.registry.remove(mapping);
			if (definition == null) {
				return;
			}

			this.mappingLookup.remove(definition.getMapping());

			for (String url : definition.getDirectUrls()) {
				List<RequestMappingInfo> list = this.urlLookup.get(url);
				if (list != null) {
					list.remove(definition.getMapping());
					if (list.isEmpty()) {
						this.urlLookup.remove(url);
					}
				}
			}
		}

		public void cleanup() {
			registry.clear();
			mappingLookup.clear();
			urlLookup.clear();
		}
	}

	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition().getPatterns();
	}

	private static class MappingRegistration<RequestMappingInfo> {

		private final RequestMappingInfo mapping;

		private final List<String> directUrls;

		public MappingRegistration(RequestMappingInfo mapping, List<String> directUrls) {
			Assert.notNull(mapping, "Mapping must not be null");
			this.mapping = mapping;
			this.directUrls = (directUrls != null ? directUrls : new ArrayList<String>());
		}

		public RequestMappingInfo getMapping() {
			return this.mapping;
		}

		public List<String> getDirectUrls() {
			return this.directUrls;
		}
	}

	/**
	 * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
	 * comparing the best match with a comparator in the context of the current request.
	 */
	private class Match {
		private final RequestMappingInfo mapping;

		public Match(RequestMappingInfo mapping) {
			this.mapping = mapping;
		}

		public RequestMappingInfo getMapping() {
			return mapping;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}

	private class MatchComparator implements Comparator<Match> {

		private final Comparator<RequestMappingInfo> comparator;

		public MatchComparator(Comparator<RequestMappingInfo> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}
}
