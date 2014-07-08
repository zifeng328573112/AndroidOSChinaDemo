package net.oschina.app.adapter;

import android.widget.BaseAdapter;

public abstract class MyBaseAdapter extends BaseAdapter {
	//标识LinkView上的链接
	private boolean isLinkViewClick = false;

	public boolean isLinkViewClick() {
		return isLinkViewClick;
	}

	public void setLinkViewClick(boolean isLinkViewClick) {
		this.isLinkViewClick = isLinkViewClick;
	}

}
