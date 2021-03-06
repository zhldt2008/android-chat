/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.WfcTextUtils;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ForwardActivity extends PickOrCreateConversationActivity {
    private Message message;
    private ForwardViewModel forwardViewModel;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;

    @Override
    protected void afterViews() {
        super.afterViews();
        message = getIntent().getParcelableExtra("message");
        forwardViewModel = ViewModelProviders.of(this).get(ForwardViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
    }

    @Override
    protected void onPickOrCreateConversation(Conversation conversation) {
        forward(conversation);
    }

    public void forward(Conversation conversation) {
        switch (conversation.type) {
            case Single:
                UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
                forward(userInfo.displayName, userInfo.portrait, conversation);
                break;
            case Group:
                GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
                forward(groupInfo.name, groupInfo.portrait, conversation);
                break;
            default:
                break;
        }

    }

    private void forward(String targetName, String targetPortrait, Conversation targetConversation) {
        ForwardPromptView view = new ForwardPromptView(this);
        if (message.content instanceof ImageMessageContent) {
            view.bind(targetName, targetPortrait, ((ImageMessageContent) message.content).getThumbnail());
        } else if (message.content instanceof VideoMessageContent) {
            view.bind(targetName, targetPortrait, ((VideoMessageContent) message.content).getThumbnail());
        } else {
            view.bind(targetName, targetPortrait, WfcTextUtils.htmlToText(message.digest()));
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .customView(view, false)
            .negativeText("取消")
            .positiveText("发送")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    Message extraMsg = null;
                    if (!TextUtils.isEmpty(view.getEditText())) {
                        TextMessageContent content = new TextMessageContent(view.getEditText());
                        extraMsg = new Message();
                        extraMsg.content = content;
                    }
                    forwardViewModel.forward(targetConversation, message, extraMsg)
                        .observe(ForwardActivity.this, new Observer<OperateResult<Integer>>() {
                            @Override
                            public void onChanged(@Nullable OperateResult<Integer> integerOperateResult) {
                                if (integerOperateResult.isSuccess()) {
                                    Toast.makeText(ForwardActivity.this, "转发成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(ForwardActivity.this, "转发失败" + integerOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                }
            })
            .build();
        dialog.show();
    }
}
