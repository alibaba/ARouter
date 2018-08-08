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

import java.util.Collection;
import java.util.Iterator;

/**
 * A base class for {@link RequestCondition} types providing implementations of
 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

	/**
	 * Indicates whether this condition is empty, i.e. whether or not it
	 * contains any discrete items.
	 * @return {@code true} if empty; {@code false} otherwise
	 */
	public boolean isEmpty() {
		return getContent().isEmpty();
	}

	/**
	 * Return the discrete items a request condition is composed of.
	 * <p>For example URL patterns, HTTP request methods, param expressions, etc.
	 * @return a collection of objects, never {@code null}
	 */
	protected abstract Collection<?> getContent();

	/**
	 * The notation to use when printing discrete items of content.
	 * <p>For example {@code " || "} for URL patterns or {@code " && "}
	 * for param expressions.
	 */
	protected abstract String getToStringInfix();


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass() == obj.getClass()) {
			AbstractRequestCondition<?> other = (AbstractRequestCondition<?>) obj;
			return getContent().equals(other.getContent());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		for (Iterator<?> iterator = getContent().iterator(); iterator.hasNext();) {
			Object expression = iterator.next();
			builder.append(expression.toString());
			if (iterator.hasNext()) {
				builder.append(getToStringInfix());
			}
		}
		builder.append("]");
		return builder.toString();
	}
}
