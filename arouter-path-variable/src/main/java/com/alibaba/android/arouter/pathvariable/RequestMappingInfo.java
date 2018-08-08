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

import com.alibaba.android.arouter.pathvariable.util.PathMatcher;
import com.alibaba.android.arouter.pathvariable.util.StringUtils;

public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {
	private final String name;

	private final PatternsRequestCondition patternsCondition;


	public RequestMappingInfo(String name, PatternsRequestCondition patterns) {
		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : new PatternsRequestCondition());
	}

	/**
	 * Creates a new instance with the given request conditions.
	 * @param patterns patterns
	 */
	public RequestMappingInfo(PatternsRequestCondition patterns) {
		this(null, patterns);
	}

	/**
	 * @return return the name for this mapping, or {@code null}.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return return the URL patterns of this {@link RequestMappingInfo};
	 * or instance with 0 patterns (never {@code null}).
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}


	/**
	 * Combine "this" request mapping info (i.e. the current instance) with another request mapping info instance.
	 * <p>Example: combine type- and method-level request mappings.
	 * @return a new request mapping info instance; never {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);

		return new RequestMappingInfo(name, patterns);
	}

	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			String separator = "#";
			return this.name + separator + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return other.name;
		}
	}

	/**
	 * Checks if all conditions in this request mapping info match the provided request and returns
	 * a potentially new request mapping info with conditions tailored to the current request.
	 * <p>For example the returned instance may contain the subset of URL patterns that match to
	 * the current request, sorted with best matching patterns on top.
	 * @return a new instance in case all conditions match; or {@code null} otherwise
	 */
	@Override
	public RequestMappingInfo getMatchingCondition(String lookupPath) {
		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(lookupPath);
		if (patterns == null) {
			return null;
		}
		return new RequestMappingInfo(this.name, patterns);
	}

	/**
	 * Compares "this" info (i.e. the current instance) with another info in the context of a request.
	 * <p>Note: It is assumed both instances have been obtained via
	 * content relevant to current request.
	 */
	@Override
	public int compareTo(RequestMappingInfo other, String request) {
		int result;

		result = this.patternsCondition.compareTo(other.getPatternsCondition(), request);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	/**
	 * @param other other
	 * @return equals
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RequestMappingInfo)) {
			return false;
		}
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return this.patternsCondition.equals(otherInfo.patternsCondition);
	}

	@Override
	public int hashCode() {
		return (this.patternsCondition.hashCode() * 31);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(this.patternsCondition);
		builder.append('}');
		return builder.toString();
	}

	/**
	 * Create a new {@code RequestMappingInfo.Builder} with the given paths.
	 * @param paths the paths to use
	 * @since 4.2
	 * @return Builder
	 */
	public static Builder paths(String... paths) {
		return new Builder(paths);
	}

	public static class Builder {

		private String[] paths = new String[0];

		private String mappingName;

		private BuilderConfiguration options = new BuilderConfiguration();

		public Builder(String... paths) {
			this.paths = paths;
		}

		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		public Builder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		public RequestMappingInfo build() {
			PatternsRequestCondition patternsCondition = new PatternsRequestCondition(
					this.paths, this.options.getPathMatcher(),
					this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch());

			return new RequestMappingInfo(this.mappingName, patternsCondition);
		}
	}

	/**
	 * Container for configuration options used for request mapping purposes.
	 * Such configuration is required to create RequestMappingInfo instances but
	 * is typically used across all RequestMappingInfo instances.
	 * @since 4.2
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		private PathMatcher pathMatcher;

		private boolean trailingSlashMatch = true;

		private boolean suffixPatternMatch = true;

		private boolean registeredSuffixPatternMatch = false;

		/**
		 * Set a custom PathMatcher to use for the PatternsRequestCondition.
		 * <p>By default this is not set.
		 * @param pathMatcher pathMatcher
		 */
		public void setPathMatcher(PathMatcher pathMatcher) {
			this.pathMatcher = pathMatcher;
		}

		/**
		 * @return a custom PathMatcher to use for the PatternsRequestCondition, if any.
		 */
		public PathMatcher getPathMatcher() {
			return this.pathMatcher;
		}

		/**
		 * Set whether to apply trailing slash matching in PatternsRequestCondition.
		 * <p>By default this is set to 'true'.
		 * @param trailingSlashMatch trailingSlashMatch
		 */
		public void setTrailingSlashMatch(boolean trailingSlashMatch) {
			this.trailingSlashMatch = trailingSlashMatch;
		}

		/**
		 * @return whether to apply trailing slash matching in PatternsRequestCondition.
		 */
		public boolean useTrailingSlashMatch() {
			return this.trailingSlashMatch;
		}

		/**
		 * Set whether to apply suffix pattern matching in PatternsRequestCondition.
		 * <p>By default this is set to 'true'.
		 * @param suffixPatternMatch suffixPatternMatch
		 * @see #setRegisteredSuffixPatternMatch(boolean)
		 */
		public void setSuffixPatternMatch(boolean suffixPatternMatch) {
			this.suffixPatternMatch = suffixPatternMatch;
		}

		/**
		 * @return whether to apply suffix pattern matching in PatternsRequestCondition.
		 */
		public boolean useSuffixPatternMatch() {
			return this.suffixPatternMatch;
		}

		/**
		 * Set whether suffix pattern matching should be restricted to registered
		 * file extensions only. Setting this property also sets
		 * {@code suffixPatternMatch=true} and requires that a
		 * obtain the registered file extensions.
		 * @param registeredSuffixPatternMatch suffixPatternMatch
		 */
		public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
			this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
			this.suffixPatternMatch = (registeredSuffixPatternMatch || this.suffixPatternMatch);
		}

		/**
		 * Return whether suffix pattern matching should be restricted to registered
		 * file extensions only.
		 * @return registeredSuffixPatternMatch
		 */
		public boolean useRegisteredSuffixPatternMatch() {
			return this.registeredSuffixPatternMatch;
		}
	}
}
