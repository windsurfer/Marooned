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

package com.abielinski.marooned.common;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.abielinski.marooned.R;

public class DialogUtils {
	public interface OnSearchListener {
		void onSearch(@Nullable String query);
	}

	public static void showSearchDialog(
			final Context context,
			final OnSearchListener listener) {
		showSearchDialog(context, R.string.action_search, listener);
	}

	public static void showSearchDialog(
			final Context context,
			final int titleRes,
			final OnSearchListener listener) {
		final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
		final EditText editText = (EditText)LayoutInflater.from(context)
				.inflate(R.layout.dialog_editbox, null);

		final TextView.OnEditorActionListener onEnter = (v, actionId, event) -> {
			performSearch(editText, listener);
			return true;
		};
		editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		editText.setOnEditorActionListener(onEnter);

		alertBuilder.setView(editText);
		alertBuilder.setTitle(titleRes);

		alertBuilder.setPositiveButton(
				R.string.action_search,
				(dialog, which) -> performSearch(editText, listener));

		alertBuilder.setNegativeButton(R.string.dialog_cancel, null);

		final AlertDialog alertDialog = alertBuilder.create();
		alertDialog.getWindow()
				.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		alertDialog.show();
	}

	private static void performSearch(
			final EditText editText,
			final OnSearchListener listener) {
		final String query = StringUtils.asciiLowercase(editText.getText().toString()).trim();
		if(StringUtils.isEmpty(query)) {
			listener.onSearch(null);
		} else {
			listener.onSearch(query);
		}
	}
}
