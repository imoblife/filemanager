package com.filemanager.view;

import com.filemanager.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * An extension to the item_filelist layout that implements the checkable interface and displays a {@link CheckBox} to the right of the standard layout.
 * @author George Venios
 *
 */
public class CheckableFileListItem extends RelativeLayout implements Checkable{
	private CheckBox mCheckbox;
	
	public CheckableFileListItem(Context context) {
		super(context);
		init();
	}
	
	public CheckableFileListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	private void init(){
		mCheckbox = new CheckBox(getContext());
		mCheckbox.setButtonDrawable(R.drawable.base_checkbox_selector);
		mCheckbox.setId(10);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(ALIGN_PARENT_RIGHT);
		params.addRule(CENTER_VERTICAL);
		mCheckbox.setChecked(false);
		mCheckbox.setClickable(false);
		mCheckbox.setFocusable(false);
		mCheckbox.setLayoutParams(params);
		
		View item = inflate(getContext(), R.layout.item_filelist, null);
		LinearLayout item_ll = (LinearLayout) item.findViewById(R.id.item_ll);
		LayoutParams p = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		p.addRule(LEFT_OF, 10);
		p.addRule(ALIGN_PARENT_LEFT);
		item.setLayoutParams(p);
		
		item_ll.addView(mCheckbox);
		addView(item);
	}

	@Override
	public boolean isChecked() {
		return mCheckbox.isChecked();
	}

	@Override
	public void setChecked(boolean checked) {
		
		mCheckbox.setChecked(checked);
//		if(checked){
//			mCheckbox.setButtonDrawable(R.drawable.base_checkbox_checked);
//		}else{
//			mCheckbox.setButtonDrawable(R.drawable.base_checkbox_unchecked);
//		}
	}

	@Override
	public void toggle() {
		mCheckbox.toggle();
	}
	
	
	
}