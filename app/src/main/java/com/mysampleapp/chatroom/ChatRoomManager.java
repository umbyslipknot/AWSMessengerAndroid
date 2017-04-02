package com.mysampleapp.chatroom;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.models.nosql.ChatRoomDO;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on 4/25/2016.
 */
public class ChatRoomManager {

    private DynamoDBMapper mapper;
    private AmazonDynamoDBClient ddbClient;

    private ChatRoomDO chatRoomDO;



    //add chat room for individual
    public void addChatRoom(String dt, IdentityManager identityManager, Set<String> recipientId, String chatRoomId){

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityManager.getCredentialsProvider());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        chatRoomDO = new ChatRoomDO();
        chatRoomDO.setUserId(identityManager.getCredentialsProvider().getCachedIdentityId());
        chatRoomDO.setChatRoomId(chatRoomId);
        chatRoomDO.setCreatedAt(dt);
        chatRoomDO.setName(identityManager.getCurrentIdentityProvider().getUserName());
        chatRoomDO.setRecipients(recipientId);
        mapper.save(chatRoomDO);
    }

    //add chat room for group chat
    public void saveNewChatRoom(String dt, CognitoCachingCredentialsProvider provider, String groupName, Set<String> recipientId, String chatRoomId) {

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        chatRoomDO = new ChatRoomDO();
        chatRoomDO.setUserId(provider.getCachedIdentityId());
        chatRoomDO.setChatRoomId(chatRoomId);
        chatRoomDO.setCreatedAt(dt);
        chatRoomDO.setName(groupName);
        chatRoomDO.setRecipients(recipientId);
        mapper.save(chatRoomDO);
    }

    public PaginatedScanList<ChatRoomDO> loadUserChatRooms(CognitoCachingCredentialsProvider provider){

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        //find current user chat room on the base of userId
        Map<String, AttributeValue> filter = new HashMap<String, AttributeValue>();
        filter.put(":userId",new AttributeValue().withS(provider.getCachedIdentityId()));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("userId=:userId OR contains(recipients,:userId)")
                .withExpressionAttributeValues(filter);



        PaginatedScanList<ChatRoomDO> scanResult = mapper.scan(ChatRoomDO.class, scanExpression);

        if(scanResult != null){
            return scanResult;
        }

        return null;
    }

    public PaginatedScanList<ChatRoomDO> getRecipientUsersList(CognitoCachingCredentialsProvider provider, String chatRoomId){

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        final Map<String,  AttributeValue> filter = new HashMap<String, AttributeValue>();
        filter.put(":chatRoomId",new AttributeValue().withS(chatRoomId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("chatRoomId = :chatRoomId")
                .withExpressionAttributeValues(filter);

        PaginatedScanList<ChatRoomDO> scanResult = mapper.scan(ChatRoomDO.class, scanExpression);

        if(scanResult != null){
            return scanResult;
        }

        return null;
    }
}
