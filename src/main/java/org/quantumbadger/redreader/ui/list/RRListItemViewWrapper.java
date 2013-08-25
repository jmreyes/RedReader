package org.quantumbadger.redreader.ui.list;

import android.graphics.Canvas;
import org.quantumbadger.redreader.ui.views.MeasurementException;
import org.quantumbadger.redreader.ui.views.RRView;
import org.quantumbadger.redreader.ui.views.touch.RRHSwipeHandler;
import org.quantumbadger.redreader.ui.views.touch.RROffsetClickHandler;
import org.quantumbadger.redreader.ui.views.touch.RRVSwipeHandler;

public class RRListItemViewWrapper extends RRListViewItem {

	private final RRView view;
	private boolean isVisible = true;

	public RRListItemViewWrapper(final RRView view) {
		this.view = view;
		view.setParent(this);
	}

	@Override
	protected void onRender(final Canvas c) {
		view.draw(c, getInnerWidth());
	}

	@Override
	public RROffsetClickHandler getClickHandler(float x, float y) {
		// TODO padding? implement this in RRView
		return null;
	}

	public RRHSwipeHandler getHSwipeHandler(float x, float y) {
		// TODO
		return null;
	}

	public RRVSwipeHandler getVSwipeHandler(float x, float y) {
		// TODO
		return null;
	}

	@Override
	protected int onMeasureByWidth(int width) {
		return view.setWidth(width);
	}

	@Override
	protected int onMeasureByHeight(int height) {
		throw new MeasurementException(this, MeasurementException.InvalidMeasurementType.HEIGHT_DETERMINED_BY_WIDTH);
	}

	@Override
	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(final boolean isVisible) {
		this.isVisible = isVisible;
	}
}