package com.gentics.mesh.core.data.page.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.parameter.PagingParameters;

public abstract class AbstractDynamicPage<T> implements Page<T> {

	protected long pageNumber;

	protected int perPage;

	protected long lowerBound;

	protected Long totalPages = null;

	protected AtomicBoolean pageFull = new AtomicBoolean(false);

	protected List<T> elementsOfPage = new ArrayList<>();

	protected AtomicBoolean hasNextPage = new AtomicBoolean();

	protected AtomicLong totalCounter = new AtomicLong();

	protected Iterator<? extends T> visibleItems;

	public AbstractDynamicPage(PagingParameters pagingInfo) {
		if (pagingInfo.getPage() < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(pagingInfo.getPage()));
		}
		if (pagingInfo.getPerPage() < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(pagingInfo.getPerPage()));
		}
		this.pageNumber = pagingInfo.getPage();
		this.perPage = pagingInfo.getPerPage();

		this.lowerBound = (pageNumber - 1) * perPage;

		if (perPage == 0) {
			this.lowerBound = 0;
		}

	}

	@Override
	public int getPerPage() {
		return perPage;
	}

	@Override
	public long getPageCount() {
		if (totalPages == null) {
			// The totalPages of the list response must be zero if the perPage parameter is also zero.
			totalPages = 0L;
			if (perPage != 0) {
				totalPages = (long) Math.ceil(getTotalElements() / (double) (perPage));
			}
		}
		return totalPages;
	}

	@Override
	public long getNumber() {
		return pageNumber;
	}

	@Override
	public long getTotalElements() {
		// Iterate over all elements to determine the total count
		while (visibleItems.hasNext()) {
			visibleItems.next();
		}
		return totalCounter.get();
	}

	@Override
	public List<? extends T> getWrappedList() {
		// Iterate over more edges if the page is not yet full and there are any more edges
		while (visibleItems.hasNext() && !pageFull.get()) {
			visibleItems.next();
		}
		return elementsOfPage;
	}

	@Override
	public boolean hasNextPage() {
		// Iterate over more items as long as the hasNextPage flag has not been set
		while (!hasNextPage.get() && visibleItems.hasNext()) {
			visibleItems.next();
		}
		return hasNextPage.get();
	}

}
