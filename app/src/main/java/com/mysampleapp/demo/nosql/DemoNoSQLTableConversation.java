package com.mysampleapp.demo.nosql;

import android.content.Context;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.util.ThreadUtils;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.mysampleapp.R;
import com.amazonaws.models.nosql.ConversationDO;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DemoNoSQLTableConversation extends DemoNoSQLTableBase {
    private static final String LOG_TAG = DemoNoSQLTableConversation.class.getSimpleName();

    /** Inner classes use this value to determine how many results to retrieve per service call. */
    private static final int RESULTS_PER_RESULT_GROUP = 40;

    /** Removing sample data removes the items in batches of the following size. */
    private static final int MAX_BATCH_SIZE_FOR_DELETE = 50;


    /********* Primary Get Query Inner Classes *********/

    public class DemoGetWithPartitionKeyAndSortKey extends DemoNoSQLOperationBase {
        private ConversationDO result;
        private boolean resultRetrieved = true;

        DemoGetWithPartitionKeyAndSortKey(final Context context) {
            super(context.getString(R.string.nosql_operation_get_by_partition_and_sort_text),
                String.format(context.getString(R.string.nosql_operation_example_get_by_partition_and_sort_text),
                    "userId", AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID(),
                    "conversationId", "demo-userId-500000"));
        }

        @Override
        public boolean executeOperation() {
            // Retrieve an item by passing the partition key using the object mapper.
            result = mapper.load(ConversationDO.class, AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID(), "demo-conversationId-500000");

            if (result != null) {
                resultRetrieved = false;
                return true;
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            if (resultRetrieved) {
                return null;
            }
            final List<DemoNoSQLResult> results = new ArrayList<>();
            results.add(new DemoNoSQLConversationResult(result));
            resultRetrieved = true;
            return results;
        }

        @Override
        public void resetResults() {
            resultRetrieved = false;
        }
    }

    /* ******** Primary Index Query Inner Classes ******** */

    public class DemoQueryWithPartitionKeyAndSortKeyCondition extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoQueryWithPartitionKeyAndSortKeyCondition(final Context context) {
            super(context.getString(R.string.nosql_operation_title_query_by_partition_and_sort_condition_text),
                  String.format(context.getString(R.string.nosql_operation_example_query_by_partition_and_sort_condition_text),
                      "userId", AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID(),
                      "conversationId", "demo-userId-500000"));
        }

        @Override
        public boolean executeOperation() {
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withS("demo-conversationId-500000"));
            final DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withRangeKeyCondition("conversationId", rangeKeyCondition)
                .withConsistentRead(false)
                .withLimit(RESULTS_PER_RESULT_GROUP);

            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Gets the next page of results from the query.
         * @return list of results, or null if there are no more results.
         */
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoQueryWithPartitionKeyOnly extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoQueryWithPartitionKeyOnly(final Context context) {
            super(context.getString(R.string.nosql_operation_title_query_by_partition_text),
                String.format(context.getString(R.string.nosql_operation_example_query_by_partition_text),
                    "userId", AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID()));
        }

        @Override
        public boolean executeOperation() {
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            final DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withConsistentRead(false)
                .withLimit(RESULTS_PER_RESULT_GROUP);

            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoQueryWithPartitionKeyAndFilter extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoQueryWithPartitionKeyAndFilter(final Context context) {
            super(context.getString(R.string.nosql_operation_title_query_by_partition_and_filter_text),
                  String.format(context.getString(R.string.nosql_operation_example_query_by_partition_and_filter_text),
                      "userId", AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID(),
                      "createdAt", "demo-createdAt-500000"));
        }

        @Override
        public boolean executeOperation() {
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            // Use an expression names Map to avoid the potential for attribute names
            // colliding with DynamoDB reserved words.
            final Map <String, String> filterExpressionAttributeNames = new HashMap<>();
            filterExpressionAttributeNames.put("#createdAt", "createdAt");

            final Map<String, AttributeValue> filterExpressionAttributeValues = new HashMap<>();
            filterExpressionAttributeValues.put(":MincreatedAt",
                new AttributeValue().withS("demo-createdAt-500000"));

            final DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withFilterExpression("#createdAt > :MincreatedAt")
                .withExpressionAttributeNames(filterExpressionAttributeNames)
                .withExpressionAttributeValues(filterExpressionAttributeValues)
                .withConsistentRead(false)
                .withLimit(RESULTS_PER_RESULT_GROUP);

            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
             resultsIterator = results.iterator();
         }
    }

    public class DemoQueryWithPartitionKeySortKeyConditionAndFilter extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoQueryWithPartitionKeySortKeyConditionAndFilter(final Context context) {
            super(context.getString(R.string.nosql_operation_title_query_by_partition_sort_condition_and_filter_text),
                  String.format(context.getString(R.string.nosql_operation_example_query_by_partition_sort_condition_and_filter_text),
                      "userId", AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID(),
                      "conversationId", "demo-userId-500000",
                      "createdAt", "demo-createdAt-500000"));
        }

        public boolean executeOperation() {
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withS("demo-conversationId-500000"));

            // Use an expression names Map to avoid the potential for attribute names
            // colliding with DynamoDB reserved words.
            final Map <String, String> filterExpressionAttributeNames = new HashMap<>();
            filterExpressionAttributeNames.put("#createdAt", "createdAt");

            final Map<String, AttributeValue> filterExpressionAttributeValues = new HashMap<>();
            filterExpressionAttributeValues.put(":MincreatedAt",
                new AttributeValue().withS("demo-createdAt-500000"));

            final DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withRangeKeyCondition("conversationId", rangeKeyCondition)
                .withFilterExpression("#createdAt > :MincreatedAt")
                .withExpressionAttributeNames(filterExpressionAttributeNames)
                .withExpressionAttributeValues(filterExpressionAttributeValues)
                .withConsistentRead(false)
                .withLimit(RESULTS_PER_RESULT_GROUP);

            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    /* ******** Secondary Named Index Query Inner Classes ******** */



    public class DemoByCreationDateQueryWithPartitionKeyAndSortKeyCondition extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;
        DemoByCreationDateQueryWithPartitionKeyAndSortKeyCondition (final Context context) {
            super(
                context.getString(R.string.nosql_operation_title_index_query_by_partition_and_sort_condition_text),
                context.getString(R.string.nosql_operation_example_index_query_by_partition_and_sort_condition_text,
                    "chatRoomId", "demo-chatRoomId-3",
                    "createdAt", "demo-createdAt-500000"));
        }

        public boolean executeOperation() {
            // Perform a query using a partition key and sort key condition.
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setChatRoomId("demo-chatRoomId-3");
            final Condition sortKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())

                .withAttributeValueList(new AttributeValue().withS("demo-createdAt-500000"));
            // Perform get using Partition key and sort key condition
            DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withRangeKeyCondition("createdAt", sortKeyCondition)
                .withConsistentRead(false);
            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoByCreationDateQueryWithPartitionKeyOnly extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoByCreationDateQueryWithPartitionKeyOnly(final Context context) {
            super(
                context.getString(R.string.nosql_operation_title_index_query_by_partition_text),
                context.getString(R.string.nosql_operation_example_index_query_by_partition_text,
                    "chatRoomId", "demo-chatRoomId-3"));
        }

        public boolean executeOperation() {
            // Perform a query using a partition key and filter condition.
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setChatRoomId("demo-chatRoomId-3");

            // Perform get using Partition key
            DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withConsistentRead(false);
            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoByCreationDateQueryWithPartitionKeyAndFilterCondition extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoByCreationDateQueryWithPartitionKeyAndFilterCondition (final Context context) {
            super(
                context.getString(R.string.nosql_operation_title_index_query_by_partition_and_filter_text),
                context.getString(R.string.nosql_operation_example_index_query_by_partition_and_filter_text,
                    "chatRoomId","demo-chatRoomId-3",
                    "conversationId", "demo-conversationId-500000"));
        }

        public boolean executeOperation() {
            // Perform a query using a partition key and filter condition.
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setChatRoomId("demo-chatRoomId-3");

            // Use an expression names Map to avoid the potential for attribute names
            // colliding with DynamoDB reserved words.
            final Map <String, String> filterExpressionAttributeNames = new HashMap<>();
            filterExpressionAttributeNames.put("#conversationId", "conversationId");

            final Map<String, AttributeValue> filterExpressionAttributeValues = new HashMap<>();
            filterExpressionAttributeValues.put(":MinconversationId",
                new AttributeValue().withS("demo-conversationId-500000"));

            // Perform get using Partition key and sort key condition
            DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withFilterExpression("#conversationId > :MinconversationId")
                .withExpressionAttributeNames(filterExpressionAttributeNames)
                .withExpressionAttributeValues(filterExpressionAttributeValues)
                .withConsistentRead(false);
            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoByCreationDateQueryWithPartitionKeySortKeyAndFilterCondition extends DemoNoSQLOperationBase {

        private PaginatedQueryList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoByCreationDateQueryWithPartitionKeySortKeyAndFilterCondition (final Context context) {
            super(
                context.getString(R.string.nosql_operation_title_index_query_by_partition_sort_condition_and_filter_text),
                context.getString(R.string.nosql_operation_example_index_query_by_partition_sort_condition_and_filter_text,
                    "chatRoomId", "demo-chatRoomId-3",
                    "createdAt", "demo-createdAt-500000",
                    "conversationId", "demo-conversationId-500000"));
        }

        public boolean executeOperation() {
            // Perform a query using a partition key, sort condition, and filter.
            final ConversationDO itemToFind = new ConversationDO();
            itemToFind.setChatRoomId("demo-chatRoomId-3");
            final Condition sortKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withS("demo-createdAt-500000"));

            // Use a map of expression names to avoid the potential for attribute names
            // colliding with DynamoDB reserved words.
            final Map<String, String> filterExpressionAttributeNames = new HashMap<>();
            filterExpressionAttributeNames.put("#conversationId", "conversationId");

            final Map<String, AttributeValue> filterExpressionAttributeValues = new HashMap<>();
            filterExpressionAttributeValues.put(":MinconversationId",
                new AttributeValue().withS("demo-conversationId-500000"));

            // Perform get using Partition key and sort key condition
            DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
                .withHashKeyValues(itemToFind)
                .withRangeKeyCondition("createdAt", sortKeyCondition)
                .withFilterExpression("#conversationId > :MinconversationId")
                .withExpressionAttributeNames(filterExpressionAttributeNames)
                .withExpressionAttributeValues(filterExpressionAttributeValues)
                .withConsistentRead(false);
            results = mapper.query(ConversationDO.class, queryExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    /********* Scan Inner Classes *********/

    public class DemoScanWithFilter extends DemoNoSQLOperationBase {

        private PaginatedScanList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoScanWithFilter(final Context context) {
            super(context.getString(R.string.nosql_operation_title_scan_with_filter),
                String.format(context.getString(R.string.nosql_operation_example_scan_with_filter),
                    "createdAt", "demo-createdAt-500000"));
        }

        @Override
        public boolean executeOperation() {
            // Use an expression names Map to avoid the potential for attribute names
            // colliding with DynamoDB reserved words.
            final Map <String, String> filterExpressionAttributeNames = new HashMap<>();
            filterExpressionAttributeNames.put("#createdAt", "createdAt");

            final Map<String, AttributeValue> filterExpressionAttributeValues = new HashMap<>();
            filterExpressionAttributeValues.put(":MincreatedAt",
                new AttributeValue().withS("demo-createdAt-500000"));
            final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#createdAt > :MincreatedAt")
                .withExpressionAttributeNames(filterExpressionAttributeNames)
                .withExpressionAttributeValues(filterExpressionAttributeValues);

            results = mapper.scan(ConversationDO.class, scanExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public boolean isScan() {
            return true;
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    public class DemoScanWithoutFilter extends DemoNoSQLOperationBase {

        private PaginatedScanList<ConversationDO> results;
        private Iterator<ConversationDO> resultsIterator;

        DemoScanWithoutFilter(final Context context) {
            super(context.getString(R.string.nosql_operation_title_scan_without_filter),
                context.getString(R.string.nosql_operation_example_scan_without_filter));
        }

        @Override
        public boolean executeOperation() {
            final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
            results = mapper.scan(ConversationDO.class, scanExpression);
            if (results != null) {
                resultsIterator = results.iterator();
                if (resultsIterator.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DemoNoSQLResult> getNextResultGroup() {
            return getNextResultsGroupFromIterator(resultsIterator);
        }

        @Override
        public boolean isScan() {
            return true;
        }

        @Override
        public void resetResults() {
            resultsIterator = results.iterator();
        }
    }

    /**
     * Helper Method to handle retrieving the next group of query results.
     * @param resultsIterator the iterator for all the results (makes a new service call for each result group).
     * @return the next list of results.
     */
    private static List<DemoNoSQLResult> getNextResultsGroupFromIterator(final Iterator<ConversationDO> resultsIterator) {
        if (!resultsIterator.hasNext()) {
            return null;
        }
        List<DemoNoSQLResult> resultGroup = new LinkedList<>();
        int itemsRetrieved = 0;
        do {
            // Retrieve the item from the paginated results.
            final ConversationDO item = resultsIterator.next();
            // Add the item to a group of results that will be displayed later.
            resultGroup.add(new DemoNoSQLConversationResult(item));
            itemsRetrieved++;
        } while ((itemsRetrieved < RESULTS_PER_RESULT_GROUP) && resultsIterator.hasNext());
        return resultGroup;
    }

    /** The DynamoDB object mapper for accessing DynamoDB. */
    private final DynamoDBMapper mapper;

    public DemoNoSQLTableConversation() {
        mapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
    }

    @Override
    public String getTableName() {
        return "Conversation";
    }

    @Override
    public String getPartitionKeyName() {
        return "Artist";
    }

    public String getPartitionKeyType() {
        return "String";
    }

    @Override
    public String getSortKeyName() {
        return "conversationId";
    }

    public String getSortKeyType() {
        return "String";
    }

    @Override
    public int getNumIndexes() {
        return 1;
    }

    @Override
    public void insertSampleData() throws AmazonClientException {
        Log.d(LOG_TAG, "Inserting Sample data.");
        final ConversationDO firstItem = new ConversationDO();

        firstItem.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
        firstItem.setConversationId("demo-conversationId-500000");
        firstItem.setChatRoomId(DemoSampleDataGenerator.getRandomPartitionSampleString("chatRoomId"));
        firstItem.setCreatedAt(
            DemoSampleDataGenerator.getRandomSampleString("createdAt"));
        firstItem.setImageUrlPath(
            DemoSampleDataGenerator.getRandomSampleString("imageUrlPath"));
        firstItem.setMessage(
            DemoSampleDataGenerator.getRandomSampleString("message"));
        AmazonClientException lastException = null;

        try {
            mapper.save(firstItem);
        } catch (final AmazonClientException ex) {
            Log.e(LOG_TAG, "Failed saving item : " + ex.getMessage(), ex);
            lastException = ex;
        }

        final ConversationDO[] items = new ConversationDO[SAMPLE_DATA_ENTRIES_PER_INSERT-1];
        for (int count = 0; count < SAMPLE_DATA_ENTRIES_PER_INSERT-1; count++) {
            final ConversationDO item = new ConversationDO();
            item.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
            item.setConversationId(DemoSampleDataGenerator.getRandomSampleString("conversationId"));
            item.setChatRoomId(DemoSampleDataGenerator.getRandomPartitionSampleString("chatRoomId"));
            item.setCreatedAt(DemoSampleDataGenerator.getRandomSampleString("createdAt"));
            item.setImageUrlPath(DemoSampleDataGenerator.getRandomSampleString("imageUrlPath"));
            item.setMessage(DemoSampleDataGenerator.getRandomSampleString("message"));

            items[count] = item;
        }
        try {
            mapper.batchSave(Arrays.asList(items));
        } catch (final AmazonClientException ex) {
            Log.e(LOG_TAG, "Failed saving item batch : " + ex.getMessage(), ex);
            lastException = ex;
        }

        if (lastException != null) {
            // Re-throw the last exception encountered to alert the user.
            throw lastException;
        }
    }

    @Override
    public void removeSampleData() throws AmazonClientException {

        final ConversationDO itemToFind = new ConversationDO();
        itemToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

        final DynamoDBQueryExpression<ConversationDO> queryExpression = new DynamoDBQueryExpression<ConversationDO>()
            .withHashKeyValues(itemToFind)
            .withConsistentRead(false)
            .withLimit(MAX_BATCH_SIZE_FOR_DELETE);

        final PaginatedQueryList<ConversationDO> results = mapper.query(ConversationDO.class, queryExpression);

        Iterator<ConversationDO> resultsIterator = results.iterator();

        AmazonClientException lastException = null;

        if (resultsIterator.hasNext()) {
            final ConversationDO item = resultsIterator.next();

            // Demonstrate deleting a single item.
            try {
                mapper.delete(item);
            } catch (final AmazonClientException ex) {
                Log.e(LOG_TAG, "Failed deleting item : " + ex.getMessage(), ex);
                lastException = ex;
            }
        }

        final List<ConversationDO> batchOfItems = new LinkedList<ConversationDO>();
        while (resultsIterator.hasNext()) {
            // Build a batch of books to delete.
            for (int index = 0; index < MAX_BATCH_SIZE_FOR_DELETE && resultsIterator.hasNext(); index++) {
                batchOfItems.add(resultsIterator.next());
            }
            try {
                // Delete a batch of items.
                mapper.batchDelete(batchOfItems);
            } catch (final AmazonClientException ex) {
                Log.e(LOG_TAG, "Failed deleting item batch : " + ex.getMessage(), ex);
                lastException = ex;
            }

            // clear the list for re-use.
            batchOfItems.clear();
        }


        if (lastException != null) {
            // Re-throw the last exception encountered to alert the user.
            // The logs contain all the exceptions that occurred during attempted delete.
            throw lastException;
        }
    }

    private List<DemoNoSQLOperationListItem> getSupportedDemoOperations(final Context context) {
        List<DemoNoSQLOperationListItem> noSQLOperationsList = new ArrayList<DemoNoSQLOperationListItem>();
        noSQLOperationsList.add(new DemoNoSQLOperationListHeader(
            context.getString(R.string.nosql_operation_header_get)));
        noSQLOperationsList.add(new DemoGetWithPartitionKeyAndSortKey(context));

        noSQLOperationsList.add(new DemoNoSQLOperationListHeader(
            context.getString(R.string.nosql_operation_header_primary_queries)));
        noSQLOperationsList.add(new DemoQueryWithPartitionKeyOnly(context));
        noSQLOperationsList.add(new DemoQueryWithPartitionKeyAndFilter(context));
        noSQLOperationsList.add(new DemoQueryWithPartitionKeyAndSortKeyCondition(context));
        noSQLOperationsList.add(new DemoQueryWithPartitionKeySortKeyConditionAndFilter(context));

        noSQLOperationsList.add(new DemoNoSQLOperationListHeader(
            context.getString(R.string.nosql_operation_header_secondary_queries, "ByCreationDate")));

        noSQLOperationsList.add(new DemoByCreationDateQueryWithPartitionKeyOnly(context));
        noSQLOperationsList.add(new DemoByCreationDateQueryWithPartitionKeyAndFilterCondition(context));
        noSQLOperationsList.add(new DemoByCreationDateQueryWithPartitionKeyAndSortKeyCondition(context));
        noSQLOperationsList.add(new DemoByCreationDateQueryWithPartitionKeySortKeyAndFilterCondition(context));
        noSQLOperationsList.add(new DemoNoSQLOperationListHeader(
            context.getString(R.string.nosql_operation_header_scan)));
        noSQLOperationsList.add(new DemoScanWithoutFilter(context));
        noSQLOperationsList.add(new DemoScanWithFilter(context));
        return noSQLOperationsList;
    }

    @Override
    public void getSupportedDemoOperations(final Context context,
                                           final SupportedDemoOperationsHandler opsHandler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DemoNoSQLOperationListItem> supportedOperations = getSupportedDemoOperations(context);
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        opsHandler.onSupportedOperationsReceived(supportedOperations);
                    }
                });
            }
        }).start();
    }
}
