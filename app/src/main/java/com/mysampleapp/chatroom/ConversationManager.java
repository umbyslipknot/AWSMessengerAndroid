package com.mysampleapp.chatroom;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ConversationDO;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * Created on 4/28/2016.
 */
public class ConversationManager {

    private ConversationDO conversationModel;
    private DynamoDBMapper mapper;

    public void addChatRoomConversation(CognitoCachingCredentialsProvider provider, String msgTxt, String dt, String chatRoomId, String imageUrl, String randomUUIDString){

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        conversationModel = new ConversationDO();
        conversationModel.setCreatedAt(dt);
        conversationModel.setChatRoomId(chatRoomId);
        conversationModel.setMessage(msgTxt);
        conversationModel.setUserId(provider.getCachedIdentityId());
        conversationModel.setConversationId(randomUUIDString);
        conversationModel.setImageUrlPath(imageUrl);
        mapper.save(conversationModel);


    }


    public PaginatedQueryList<ConversationDO> getChatRoomConversation(CognitoCachingCredentialsProvider provider, String chatRoomId) {

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        conversationModel = new ConversationDO();
        conversationModel.setChatRoomId(chatRoomId);

        DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                .withHashKeyValues(conversationModel)
                .withIndexName("ByCreationDate")
                .withConsistentRead(false);

        PaginatedQueryList<ConversationDO> result = mapper.query(ConversationDO.class, queryExpression);
        return result;
    }
}
