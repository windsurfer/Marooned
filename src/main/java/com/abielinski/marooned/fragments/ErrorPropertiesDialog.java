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

package com.abielinski.marooned.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.abielinski.marooned.R;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.activities.BugReportActivity;
import com.abielinski.marooned.common.RRError;

import java.net.UnknownHostException;

public final class ErrorPropertiesDialog extends PropertiesDialog {

	private AppCompatActivity mContext;
	@NonNull private final RRError mError;

	private ErrorPropertiesDialog(@NonNull final RRError error) {
		mError = error;
	}

	public static ErrorPropertiesDialog newInstance(final RRError error) {

		final ErrorPropertiesDialog dialog = new ErrorPropertiesDialog(error);

		final Bundle args = new Bundle();

		args.putString("title", error.title);
		args.putString("message", error.message);

		if(error.t != null) {
			final StringBuilder sb = new StringBuilder(1024);
			BugReportActivity.appendException(sb, error.t, 10);
			args.putString("t", sb.toString());
		}

		if(error.httpStatus != null) {
			args.putString("httpStatus", error.httpStatus.toString());
		}

		if(error.url != null) {
			args.putString("url", error.url);
		}

		dialog.setArguments(args);

		return dialog;
	}

	@Override
	protected void interceptBuilder(@NonNull final AlertDialog.Builder builder) {

		if(!(mError.t instanceof UnknownHostException)) {

			builder.setPositiveButton(
					R.string.button_error_send_report,
					(dialog, which) -> BugReportActivity.sendBugReport(mContext, mError));
		}
	}

	@Override
	protected String getTitle(final Context context) {
		return context.getString(R.string.props_error_title);
	}

	@Override
	protected void prepare(
			@NonNull final BaseActivity context,
			@NonNull final LinearLayout items) {

		mContext = context;

		items.addView(propView(
				context,
				R.string.props_title,
				getArguments().getString("title"),
				true));
		items.addView(propView(
				context,
				"Message",
				getArguments().getString("message"),
				false));

		if(getArguments().containsKey("httpStatus")) {
			items.addView(propView(
					context,
					"HTTP status",
					getArguments().getString("httpStatus"),
					false));
		}

		if(getArguments().containsKey("url")) {
			items.addView(propView(
					context,
					"URL",
					getArguments().getString("url"),
					false));
		}

		if(getArguments().containsKey("t")) {
			items.addView(propView(
					context,
					"Exception",
					getArguments().getString("t"),
					false));
		}
	}
}
