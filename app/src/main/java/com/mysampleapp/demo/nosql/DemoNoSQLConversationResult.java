package com.mysampleapp.demo.nosql;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.ConversationDO;

import java.util.Set;

public class DemoNoSQLConversationResult implements DemoNoSQLResult {
    private static final int KEY_TEXT_COLOR = 0xFF333333;
    private final ConversationDO result;

    DemoNoSQLConversationResult(final ConversationDO result) {
        this.result = result;
    }
    @Override
    public void updateItem() {
        final DynamoDBMapper mapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        final String originalValue = result.getChatRoomId();
        result.setChatRoomId(DemoSampleDataGenerator.getRandomSampleString("chatRoomId"));
        try {
            mapper.save(result);
        } catch (final AmazonClientException ex) {
            // Restore original data if save fails, and re-throw.
            result.setChatRoomId(originalValue);
            throw ex;
        }
    }

    @Override
    public void deleteItem() {
        final DynamoDBMapper mapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        mapper.delete(result);
    }

    private void setKeyTextViewStyle(final TextView textView) {
        textView.setTextColor(KEY_TEXT_COLOR);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(dp(5), dp(2), dp(5), 0);
        textView.setLayoutParams(layoutParams);
    }

    /**
     * @param dp number of design pixels.
     * @return number of pixels corresponding to the desired design pixels.
     */
    private int dp(int dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
    private void setValueTextViewStyle(final TextView textView) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(dp(15), 0, dp(15), dp(2));
        textView.setLayoutParams(layoutParams);
    }

    private void setKeyAndValueTextViewStyles(final TextView keyTextView, final TextView valueTextView) {
        setKeyTextViewStyle(keyTextView);
        setValueTextViewStyle(valueTextView);
    }

    private static String bytesToHexString(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("%02X", bytes[0]));
        for(int index = 1; index < bytes.length; index++) {
            builder.append(String.format(" %02X", bytes[index]));
        }
        return builder.toString();
    }

    private static String byteSetsToHexStrings(Set<byte[]> bytesSet) {
        final StringBuilder builder = new StringBuilder();
        int index = 0;
        for (byte[] bytes : bytesSet) {
            builder.append(String.format("%d: ", ++index));
            builder.append(bytesToHexString(bytes));
            if (index < bytesSet.size()) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    @Override
    public View getView(final Context context, final View convertView, int position) {
        final LinearLayout layout;
        final TextView resultNumberTextView;
        final TextView userIdKeyTextView;
        final TextView userIdValueTextView;
        final TextView conversationIdKeyTextView;
        final TextView conversationIdValueTextView;
        final TextView chatRoomIdKeyTextView;
        final TextView chatRoomIdValueTextView;
        final TextView createdAtKeyTextView;
        final TextView createdAtValueTextView;
        final TextView imageUrlPathKeyTextView;
        final TextView imageUrlPathValueTextView;
        final TextView messageKeyTextView;
        final TextView messageValueTextView;
        if (convertView == null) {
            layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            resultNumberTextView = new TextView(context);
            resultNumberTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.addView(resultNumberTextView);


            userIdKeyTextView = new TextView(context);
            userIdValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(userIdKeyTextView, userIdValueTextView);
            layout.addView(userIdKeyTextView);
            layout.addView(userIdValueTextView);

            conversationIdKeyTextView = new TextView(context);
            conversationIdValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(conversationIdKeyTextView, conversationIdValueTextView);
            layout.addView(conversationIdKeyTextView);
            layout.addView(conversationIdValueTextView);

            chatRoomIdKeyTextView = new TextView(context);
            chatRoomIdValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(chatRoomIdKeyTextView, chatRoomIdValueTextView);
            layout.addView(chatRoomIdKeyTextView);
            layout.addView(chatRoomIdValueTextView);

            createdAtKeyTextView = new TextView(context);
            createdAtValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(createdAtKeyTextView, createdAtValueTextView);
            layout.addView(createdAtKeyTextView);
            layout.addView(createdAtValueTextView);

            imageUrlPathKeyTextView = new TextView(context);
            imageUrlPathValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(imageUrlPathKeyTextView, imageUrlPathValueTextView);
            layout.addView(imageUrlPathKeyTextView);
            layout.addView(imageUrlPathValueTextView);

            messageKeyTextView = new TextView(context);
            messageValueTextView = new TextView(context);
            setKeyAndValueTextViewStyles(messageKeyTextView, messageValueTextView);
            layout.addView(messageKeyTextView);
            layout.addView(messageValueTextView);
        } else {
            layout = (LinearLayout) convertView;
            resultNumberTextView = (TextView) layout.getChildAt(0);

            userIdKeyTextView = (TextView) layout.getChildAt(1);
            userIdValueTextView = (TextView) layout.getChildAt(2);

            conversationIdKeyTextView = (TextView) layout.getChildAt(3);
            conversationIdValueTextView = (TextView) layout.getChildAt(4);

            chatRoomIdKeyTextView = (TextView) layout.getChildAt(5);
            chatRoomIdValueTextView = (TextView) layout.getChildAt(6);

            createdAtKeyTextView = (TextView) layout.getChildAt(7);
            createdAtValueTextView = (TextView) layout.getChildAt(8);

            imageUrlPathKeyTextView = (TextView) layout.getChildAt(9);
            imageUrlPathValueTextView = (TextView) layout.getChildAt(10);

            messageKeyTextView = (TextView) layout.getChildAt(11);
            messageValueTextView = (TextView) layout.getChildAt(12);
        }

        resultNumberTextView.setText(String.format("#%d", + position+1));
        userIdKeyTextView.setText("userId");
        userIdValueTextView.setText(result.getUserId());
        conversationIdKeyTextView.setText("conversationId");
        conversationIdValueTextView.setText(result.getConversationId());
        chatRoomIdKeyTextView.setText("chatRoomId");
        chatRoomIdValueTextView.setText(result.getChatRoomId());
        createdAtKeyTextView.setText("createdAt");
        createdAtValueTextView.setText(result.getCreatedAt());
        imageUrlPathKeyTextView.setText("imageUrlPath");
        imageUrlPathValueTextView.setText(result.getImageUrlPath());
        messageKeyTextView.setText("message");
        messageValueTextView.setText(result.getMessage());
        return layout;
    }
}
