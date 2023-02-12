package com.xsb.xj.util;
/** An object to be notified of lazy XJComponent refreshes. 
This is used by lists and trees to try to keep their previous selections */
public interface LazyRefreshListener{
	public void willRefresh();
	public void didRefresh();
}
