package framework.telegram.ui.menu;

import android.view.View;

/**
 * Created by xiaoqi on 2017/12/19.
 */

public class MenuItem {

	private String item;
	private int itemResId = View.NO_ID;

	public MenuItem(){
	}

	public MenuItem(String item,int itemResId){
		this.item = item;
		this.itemResId = itemResId;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public int getItemResId() {
		return itemResId;
	}

	public void setItemResId(int itemResId) {
		this.itemResId = itemResId;
	}
}
