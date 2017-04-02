package com.mysampleapp.chatroom;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.models.nosql.ChatRoomDO;
import com.amazonaws.models.nosql.UserProfileDO;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created on 4/26/2016.
 */
public class ChatUserProfileManager {

    private UserProfileDO userProfileModel;
    private DynamoDBMapper mapper;
    private AmazonDynamoDBClient ddbClient;
    QueryResult queryResult;

    //retrive results from database table on the base of condition
    public QueryResult isUserExist(CognitoCachingCredentialsProvider provider){

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        String id = provider.getCachedIdentityId();

        //condition : where given id match
        Condition hashKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withS(id));


        Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put("userId", hashKeyCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        do {
            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(AWSConfiguration.AMAZON_DYNAMODB_TABLENAME_USERPROFILE)
                    .withKeyConditions(keyConditions)
                    .withExclusiveStartKey(lastEvaluatedKey);

             queryResult =  ddbClient.query(queryRequest);

            // If the response lastEvaluatedKey has contents, that means there are more results
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
        } while (lastEvaluatedKey != null);

        return queryResult;
    }


    public void addUserProfile(Set<String> endpointArn, String ph, IdentityManager identityManager, CognitoCachingCredentialsProvider cachingCredentialsProvider){

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(cachingCredentialsProvider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        userProfileModel = new UserProfileDO();
        userProfileModel.setName(identityManager.getCurrentIdentityProvider().getUserName());
        userProfileModel.setUserId(cachingCredentialsProvider.getCachedIdentityId());
        userProfileModel.setPhone(ph);
        userProfileModel.setPushTargetArn(endpointArn);
        mapper.save(userProfileModel);
    }

    //return selected user object
    public PaginatedScanList<UserProfileDO> awsUserExist(CognitoCachingCredentialsProvider provider, String phNo){

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        Map<String, Condition> filter = new HashMap<String, Condition>();

        //condition : where given phone number match
        filter.put("phone",new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(new AttributeValue().withS(phNo)));

        scanExpression.setScanFilter(filter);
        PaginatedScanList<UserProfileDO> scanResult = mapper.scan(UserProfileDO.class, scanExpression);

        if(scanResult != null){
            return scanResult;
        }

        return null;
    }


    public PaginatedScanList<UserProfileDO> getUserProfileList(CognitoCachingCredentialsProvider provider, final PaginatedScanList<ChatRoomDO> usersList) {

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);


        final Map<String,  AttributeValue> filter = new HashMap<String, AttributeValue>();

        int j=0;
       //get  each chat room recipients set and add it into array list
        for (int i=0; i<usersList.size(); i++ ) {
           Set<String> recipientsId = usersList.get(i).getRecipients();
            filter.put(":val"+j++,new AttributeValue().withS(usersList.get(i).getUserId().toString()));
            // create an iterator
            Iterator iterator = recipientsId.iterator();
            for( ;iterator.hasNext();){
                filter.put(":val"+j++,new AttributeValue().withS(iterator.next().toString()));
            }
        }

        Set<String> filterkeySet = filter.keySet();
        String newSetStr = null ;
        for(String str : filterkeySet){
            if(newSetStr == null){
                newSetStr = str;
            }else{
                newSetStr += "," + str;
            }
        }

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("userId in ("+newSetStr+")")
                .withExpressionAttributeValues(filter);

        PaginatedScanList<UserProfileDO> scanResult  = mapper.scan(UserProfileDO.class, scanExpression);

        return scanResult;
    }


    public PaginatedScanList<UserProfileDO> loadUsersWithPhoneList(CognitoCachingCredentialsProvider provider, Object[] phNoList) {

        ddbClient = new AmazonDynamoDBClient(provider.getCredentials());
        ddbClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_DYNAMODB_REGION));
        mapper = new DynamoDBMapper(ddbClient);

        Map<String,  AttributeValue> filter = new HashMap<String, AttributeValue>();

        //search users on the base of phone number
        int i = 0;
        for ( Object obj : phNoList ) {
            filter.put(":val"+i++,new AttributeValue().withS(obj.toString()));
        }
        Set<String> filterkeySet = filter.keySet();
        String newSetStr = null ;
        for(String str : filterkeySet){
            if(newSetStr == null){
                newSetStr = str;
            }else{
                newSetStr += "," + str;
            }
        }

        final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("phone in ("+newSetStr+")")
                .withExpressionAttributeValues(filter);

        PaginatedScanList<UserProfileDO> scanResult  = mapper.scan(UserProfileDO.class, scanExpression);
        if(scanResult != null){
            return scanResult;
        }

        return null;

    }

}
