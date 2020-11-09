/*******************************************************************************
 * This file is part of Marooned.
 *
 * Marooned is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Marooned is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Marooned.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.abielinski.marooned.views.liststatus;

import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.abielinski.marooned.R;
import com.abielinski.marooned.activities.RefreshableActivity;
import com.abielinski.marooned.common.RRError;
import com.abielinski.marooned.fragments.ErrorPropertiesDialog;
import com.abielinski.marooned.views.glview.Refreshable;

import java.util.Locale;

public final class ErrorView extends StatusListItemView {

	public ErrorView(final AppCompatActivity activity, final RRError error) {

		super(activity);

		final TextView textView = new TextView(activity);
		textView.setText(error.title);
		textView.setTextColor(Color.WHITE);
		textView.setTextSize(18.0f);
		textView.setPadding(0, 0, 0, (int)(4 * dpScale));

		final TextView messageView = new TextView(activity);
		messageView.setText(error.message);

		messageView.setTextColor(Color.WHITE);
		messageView.setTextSize(14.0f);
		messageView.setPadding(0, 0, 0, (int)(15 * dpScale));

		final Button detailsButton = new Button(activity);
		detailsButton.setTextColor(Color.WHITE);
		detailsButton.setText(activity.getApplicationContext()
				.getString(R.string.button_error_details)
				.toUpperCase(Locale.getDefault()));
		detailsButton.setBackgroundColor(Color.rgb(0x36, 0x36, 0x3F));

		detailsButton.setOnClickListener(v -> ErrorPropertiesDialog.newInstance(error)
				.show(
						activity.getSupportFragmentManager(),
						null));

		final Button refreshButton = new Button(activity);
		refreshButton.setTextColor(Color.WHITE);
		refreshButton.setText(activity.getApplicationContext()
				.getString(R.string.options_refresh)
				.toUpperCase(Locale.getDefault()));
		refreshButton.setBackgroundColor(Color.rgb(0x36, 0x36, 0x3F));

		refreshButton.setOnClickListener(v -> {
			if ( activity instanceof RefreshableActivity ){
				((RefreshableActivity)activity).requestRefresh(RefreshableActivity.RefreshableFragment.RESTART, false);
			}else{
				refreshButton.setVisibility(INVISIBLE);
			}
		});
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(24, 0, 0, 0);



		final LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setHorizontalGravity(Gravity.RIGHT);
		layout.addView(textView);
		layout.addView(messageView);

		final LinearLayout buttonLayout = new LinearLayout(activity);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

		buttonLayout.addView(detailsButton);
		buttonLayout.addView(refreshButton,layoutParams);
		detailsButton.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
		refreshButton.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

		layout.addView(buttonLayout);

		layout.setPadding(
				(int)(15 * dpScale),
				(int)(15 * dpScale),
				(int)(15 * dpScale),
				(int)(15 * dpScale));


		setContents(layout);

		setBackgroundColor(Color.argb(0xDD,0x27, 0x27, 0x27));
	}
}
