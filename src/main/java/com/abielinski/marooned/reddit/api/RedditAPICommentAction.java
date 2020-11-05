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

package com.abielinski.marooned.Reddit.api;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipboardManager;
import android.widget.Toast;
import org.apache.commons.text.StringEscapeUtils;
import com.abielinski.marooned.R;
import com.abielinski.marooned.account.RedditAccount;
import com.abielinski.marooned.account.RedditAccountManager;
import com.abielinski.marooned.activities.CommentEditActivity;
import com.abielinski.marooned.activities.CommentReplyActivity;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.common.AndroidCommon;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.LinkHandler;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.common.RRError;
import com.abielinski.marooned.common.RRTime;
import com.abielinski.marooned.fragments.CommentListingFragment;
import com.abielinski.marooned.fragments.CommentPropertiesDialog;
import com.abielinski.marooned.Reddit.APIResponseHandler;
import com.abielinski.marooned.Reddit.RedditAPI;
import com.abielinski.marooned.Reddit.prepared.RedditChangeDataManager;
import com.abielinski.marooned.Reddit.prepared.RedditRenderableComment;
import com.abielinski.marooned.Reddit.things.RedditComment;
import com.abielinski.marooned.Reddit.url.UserProfileURL;
import com.abielinski.marooned.views.RedditCommentView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class RedditAPICommentAction {

	public enum RedditCommentAction {
		UPVOTE,
		UNVOTE,
		DOWNVOTE,
		SAVE,
		UNSAVE,
		REPORT,
		SHARE,
		COPY_TEXT,
		COPY_URL,
		REPLY,
		USER_PROFILE,
		COMMENT_LINKS,
		COLLAPSE,
		EDIT,
		DELETE,
		PROPERTIES,
		CONTEXT,
		GO_TO_COMMENT,
		ACTION_MENU,
		BACK
	}

	private static class RCVMenuItem {
		public final String title;
		public final RedditCommentAction action;

		private RCVMenuItem(
				final Context context,
				final int titleRes,
				final RedditCommentAction action) {

			this.title = context.getString(titleRes);
			this.action = action;
		}
	}

	public static void showActionMenu(
			final AppCompatActivity activity,
			final CommentListingFragment commentListingFragment,
			final RedditRenderableComment comment,
			final RedditCommentView commentView,
			final RedditChangeDataManager changeDataManager,
			final boolean isArchived) {

		final RedditAccount user =
				RedditAccountManager.getInstance(activity).getDefaultAccount();

		final ArrayList<RCVMenuItem> menu = new ArrayList<>();

		if(!user.isAnonymous()) {

			if(!isArchived) {
				if(!changeDataManager.isUpvoted(comment)) {
					menu.add(new RCVMenuItem(
							activity,
							R.string.action_upvote,
							RedditCommentAction.UPVOTE));
				} else {
					menu.add(new RCVMenuItem(
							activity,
							R.string.action_upvote_remove,
							RedditCommentAction.UNVOTE));
				}

				if(!changeDataManager.isDownvoted(comment)) {
					menu.add(new RCVMenuItem(
							activity,
							R.string.action_downvote,
							RedditCommentAction.DOWNVOTE));
				} else {
					menu.add(new RCVMenuItem(
							activity,
							R.string.action_downvote_remove,
							RedditCommentAction.UNVOTE));
				}
			}

			if(changeDataManager.isSaved(comment)) {
				menu.add(new RCVMenuItem(
						activity,
						R.string.action_unsave,
						RedditCommentAction.UNSAVE));
			} else {
				menu.add(new RCVMenuItem(
						activity,
						R.string.action_save,
						RedditCommentAction.SAVE));
			}

			menu.add(new RCVMenuItem(
					activity,
					R.string.action_report,
					RedditCommentAction.REPORT));

			if(!isArchived) {
				menu.add(new RCVMenuItem(
						activity,
						R.string.action_reply,
						RedditCommentAction.REPLY));
			}

			if(user.username.equalsIgnoreCase(comment.getParsedComment()
					.getRawComment().author)) {
				if(!isArchived) {
					menu.add(new RCVMenuItem(
							activity,
							R.string.action_edit,
							RedditCommentAction.EDIT));
				}
				menu.add(new RCVMenuItem(
						activity,
						R.string.action_delete,
						RedditCommentAction.DELETE));
			}
		}

		menu.add(new RCVMenuItem(
				activity,
				R.string.action_comment_context,
				RedditCommentAction.CONTEXT));
		menu.add(new RCVMenuItem(
				activity,
				R.string.action_comment_go_to,
				RedditCommentAction.GO_TO_COMMENT));

		menu.add(new RCVMenuItem(
				activity,
				R.string.action_comment_links,
				RedditCommentAction.COMMENT_LINKS));

		if(commentListingFragment != null) {
			menu.add(new RCVMenuItem(
					activity,
					R.string.action_collapse,
					RedditCommentAction.COLLAPSE));
		}

		menu.add(new RCVMenuItem(
				activity,
				R.string.action_share,
				RedditCommentAction.SHARE));
		menu.add(new RCVMenuItem(
				activity,
				R.string.action_copy_text,
				RedditCommentAction.COPY_TEXT));
		menu.add(new RCVMenuItem(
				activity,
				R.string.action_copy_link,
				RedditCommentAction.COPY_URL));
		menu.add(new RCVMenuItem(
				activity,
				R.string.action_user_profile,
				RedditCommentAction.USER_PROFILE));
		menu.add(new RCVMenuItem(
				activity,
				R.string.action_properties,
				RedditCommentAction.PROPERTIES));

		final String[] menuText = new String[menu.size()];

		for(int i = 0; i < menuText.length; i++) {
			menuText[i] = menu.get(i).title;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setItems(menuText, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				onActionMenuItemSelected(
						comment,
						commentView,
						activity,
						commentListingFragment,
						menu.get(which).action,
						changeDataManager);
			}
		});

		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public static void onActionMenuItemSelected(
			final RedditRenderableComment renderableComment,
			final RedditCommentView commentView,
			final AppCompatActivity activity,
			final CommentListingFragment commentListingFragment,
			final RedditCommentAction action,
			final RedditChangeDataManager changeDataManager) {

		final RedditComment comment =
				renderableComment.getParsedComment().getRawComment();

		switch(action) {

			case UPVOTE:
				action(activity, comment, RedditAPI.ACTION_UPVOTE, changeDataManager);
				break;

			case DOWNVOTE:
				action(activity, comment, RedditAPI.ACTION_DOWNVOTE, changeDataManager);
				break;

			case UNVOTE:
				action(activity, comment, RedditAPI.ACTION_UNVOTE, changeDataManager);
				break;

			case SAVE:
				action(activity, comment, RedditAPI.ACTION_SAVE, changeDataManager);
				break;

			case UNSAVE:
				action(activity, comment, RedditAPI.ACTION_UNSAVE, changeDataManager);
				break;

			case REPORT:

				new AlertDialog.Builder(activity)
						.setTitle(R.string.action_report)
						.setMessage(R.string.action_report_sure)
						.setPositiveButton(
								R.string.action_report,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {
										action(
												activity,
												comment,
												RedditAPI.ACTION_REPORT,
												changeDataManager);
									}
								})
						.setNegativeButton(R.string.dialog_cancel, null)
						.show();

				break;

			case REPLY: {
				final Intent intent = new Intent(activity, CommentReplyActivity.class);
				intent.putExtra(
						CommentReplyActivity.PARENT_ID_AND_TYPE_KEY,
						comment.getIdAndType());
				intent.putExtra(
						CommentReplyActivity.PARENT_MARKDOWN_KEY,
						StringEscapeUtils.unescapeHtml4(comment.body));
				activity.startActivity(intent);
				break;
			}

			case EDIT: {
				final Intent intent = new Intent(activity, CommentEditActivity.class);
				intent.putExtra("commentIdAndType", comment.getIdAndType());
				intent.putExtra(
						"commentText",
						StringEscapeUtils.unescapeHtml4(comment.body));
				activity.startActivity(intent);
				break;
			}

			case DELETE: {
				new AlertDialog.Builder(activity)
						.setTitle(R.string.accounts_delete)
						.setMessage(R.string.delete_confirm)
						.setPositiveButton(
								R.string.action_delete,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {
										action(
												activity,
												comment,
												RedditAPI.ACTION_DELETE,
												changeDataManager);
									}
								})
						.setNegativeButton(R.string.dialog_cancel, null)
						.show();
				break;
			}

			case COMMENT_LINKS:
				final HashSet<String> linksInComment = comment.computeAllLinks();

				if(linksInComment.isEmpty()) {
					General.quickToast(activity, R.string.error_toast_no_urls_in_comment);

				} else {

					final String[] linksArr =
							linksInComment.toArray(new String[linksInComment.size()]);

					final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setItems(linksArr, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							LinkHandler.onLinkClicked(activity, linksArr[which], false);
							dialog.dismiss();
						}
					});

					final AlertDialog alert = builder.create();
					alert.setTitle(R.string.action_comment_links);
					alert.setCanceledOnTouchOutside(true);
					alert.show();
				}

				break;

			case SHARE: {

				String body = "";
				String subject = null;

				if(PrefsUtility.pref_behaviour_sharing_include_desc(
						activity,
						PreferenceManager.getDefaultSharedPreferences(activity))) {
					subject = String.format(
							Locale.US,
							activity.getText(R.string.share_comment_by_on_reddit)
									.toString(),
							comment.author);
				}

				// TODO this currently just dumps the markdown (only if sharing text is enabled)
				if(PrefsUtility.pref_behaviour_sharing_share_text(
						activity,
						PreferenceManager.getDefaultSharedPreferences(activity))) {
					body = StringEscapeUtils.unescapeHtml4(comment.body)
							+ "\r\n\r\n";
				}

				body += comment.getContextUrl().generateNonJsonUri().toString();

				LinkHandler.shareText(activity, subject, body);
				break;
			}

			case COPY_TEXT: {
				final ClipboardManager clipboardManager =
						(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
				// TODO this currently just dumps the markdown
				if(clipboardManager != null) {
					final ClipData data = ClipData.newPlainText(
							null,
							StringEscapeUtils.unescapeHtml4(comment.body));
					clipboardManager.setPrimaryClip(data);

					General.quickToast(
							activity.getApplicationContext(),
							R.string.comment_text_copied_to_clipboard);
				}
				break;
			}

			case COPY_URL: {
				final ClipboardManager clipboardManager =
						(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
				if(clipboardManager != null) {
					final ClipData data = ClipData.newRawUri(
							null,
							comment.getContextUrl().context(null).generateNonJsonUri());
					clipboardManager.setPrimaryClip(data);

					General.quickToast(
							activity.getApplicationContext(),
							R.string.comment_link_copied_to_clipboard);
				}
				break;
			}

			case COLLAPSE: {
				commentListingFragment.handleCommentVisibilityToggle(commentView);
				break;
			}

			case USER_PROFILE:
				LinkHandler.onLinkClicked(
						activity,
						new UserProfileURL(comment.author).toString());
				break;

			case PROPERTIES:
				CommentPropertiesDialog.newInstance(comment)
						.show(activity.getSupportFragmentManager(), null);
				break;

			case GO_TO_COMMENT: {
				LinkHandler.onLinkClicked(
						activity,
						comment.getContextUrl().context(null).toString());
				break;
			}

			case CONTEXT: {
				LinkHandler.onLinkClicked(activity, comment.getContextUrl().toString());
				break;
			}
			case ACTION_MENU:
				showActionMenu(
						activity,
						commentListingFragment,
						renderableComment,
						commentView,
						changeDataManager,
						comment.isArchived());
				break;

			case BACK:
				activity.onBackPressed();
				break;
		}
	}

	public static void action(
			final AppCompatActivity activity,
			final RedditComment comment,
			final @RedditAPI.RedditAction int action,
			final RedditChangeDataManager changeDataManager) {

		final RedditAccount user =
				RedditAccountManager.getInstance(activity).getDefaultAccount();

		if(user.isAnonymous()) {
			General.showMustBeLoggedInDialog(activity);
			return;
		}

		final boolean wasUpvoted = changeDataManager.isUpvoted(comment);
		final boolean wasDownvoted = changeDataManager.isUpvoted(comment);

		switch(action) {
			case RedditAPI.ACTION_DOWNVOTE:
				if(!comment.isArchived()) {
					changeDataManager.markDownvoted(
							RRTime.utcCurrentTimeMillis(),
							comment);
				}
				break;
			case RedditAPI.ACTION_UNVOTE:
				if(!comment.isArchived()) {
					changeDataManager.markUnvoted(RRTime.utcCurrentTimeMillis(), comment);
				}
				break;
			case RedditAPI.ACTION_UPVOTE:
				if(!comment.isArchived()) {
					changeDataManager.markUpvoted(RRTime.utcCurrentTimeMillis(), comment);
				}
				break;
			case RedditAPI.ACTION_SAVE:
				changeDataManager.markSaved(RRTime.utcCurrentTimeMillis(), comment, true);
				break;
			case RedditAPI.ACTION_UNSAVE:
				changeDataManager.markSaved(
						RRTime.utcCurrentTimeMillis(),
						comment,
						false);
				break;
		}

		final boolean vote = (action == RedditAPI.ACTION_DOWNVOTE
				| action == RedditAPI.ACTION_UPVOTE
				| action == RedditAPI.ACTION_UNVOTE);

		if(comment.isArchived() && vote) {
			Toast.makeText(activity, R.string.error_archived_vote, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		RedditAPI.action(CacheManager.getInstance(activity),
				new APIResponseHandler.ActionResponseHandler(activity) {
					@Override
					protected void onCallbackException(final Throwable t) {
						throw new RuntimeException(t);
					}

					@Override
					protected void onFailure(
							final @CacheRequest.RequestFailureType int type,
							final Throwable t,
							final Integer status,
							final String readableMessage) {
						revertOnFailure();
						if(t != null) {
							t.printStackTrace();
						}

						final RRError error = General.getGeneralErrorForFailure(
								context,
								type,
								t,
								status,
								null);
						AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
							@Override
							public void run() {
								General.showResultDialog(activity, error);
							}
						});
					}

					@Override
					protected void onFailure(final APIFailureType type) {
						revertOnFailure();

						final RRError error =
								General.getGeneralErrorForFailure(context, type);
						AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
							@Override
							public void run() {
								General.showResultDialog(activity, error);
							}
						});
					}

					@Override
					protected void onSuccess(@Nullable final String redirectUrl) {
						if(action == RedditAPI.ACTION_DELETE) {
							General.quickToast(context, R.string.delete_success);
						}
					}

					private void revertOnFailure() {

						switch(action) {
							case RedditAPI.ACTION_DOWNVOTE:
							case RedditAPI.ACTION_UNVOTE:
							case RedditAPI.ACTION_UPVOTE:
								if(wasUpvoted) {
									changeDataManager.markUpvoted(
											RRTime.utcCurrentTimeMillis(),
											comment);
								} else if(wasDownvoted) {
									changeDataManager.markDownvoted(
											RRTime.utcCurrentTimeMillis(),
											comment);
								} else {
									changeDataManager.markUnvoted(
											RRTime.utcCurrentTimeMillis(),
											comment);
								}
							case RedditAPI.ACTION_SAVE:
								changeDataManager.markSaved(
										RRTime.utcCurrentTimeMillis(),
										comment,
										false);
								break;
							case RedditAPI.ACTION_UNSAVE:
								changeDataManager.markSaved(
										RRTime.utcCurrentTimeMillis(),
										comment,
										true);
								break;
						}
					}

				}, user, comment.getIdAndType(), action, activity);
	}
}
