# Realtime data migrate to hybriddb for postgresql

## Summary
0. [Introduction](#introduction)
1. [Architecture](#architecture)
2. [Configure Oss trigger for Function Compute](#configure-oss-trigger-for-function-compute)
3. [Code Practice](#code-practice)
4. [Support](#support)

## Introduction
This document introduces how to use FunctionCompute,PostgreySQL,Java and other skills to migrate data realtime. If you want to know how to migrate full data from aws redshift to hybriddn for
postgreysql, you can visit this
[address](https://github.com/alibabacloud-howto/migrate_aws_redshift_to_hybriddb_for_postgresql). Here this docoment will tell you how to migrate realtime data to hybriddb for
postgreysql.

## Architecture

![](images/4b0a19f9382c8d58916de04068d8ffa7.png)

- You should write your increment data to oss directly, for example, maybe you
    have generate some produce data, the save type is csv file. you can write
    these csv files to oss by oss API

- Oss will listen to the modify of bucket, and notice function compute to
    process data.

- Function Compute will start to process data, you can use python/nodejs/java
    or other language to process, here I use java.

## Configure Oss Trigger for Function Compute

### Create Oss Bucket
Before you follow these steps to create an Object Storage Service (OSS) bucket,
make sure that you have activated OSS:

- Log on to the [OSS console](https://oss.console.aliyun.com/).

- See *OSS topic* [Create a
    bucket](https://www.alibabacloud.com/help/doc-detail/31885.htm) to create a
    bucket.
In this sample, we select the China East 2 (Shanghai) region, set the name of the OSS bucket to awesomefc, set **Storage Class** to **Standard Storage** and set **ACL** to **Private**.

- click the **Files** tab, click **Create Directory**, and set the directory name to **source**. Click **OK**.

- In the /source directory, upload an image. In this example, the
    serverless.png image is uploaded.

### Create Function

- Log on to the [Function Compute console](https://fc.console.aliyun.com/).

- See topic [Service operations](https://www.alibabacloud.com/help/doc-detail/73337.htm) to
    create a service.

    In this example, we select the China East 2 (Shanghai) region, set the service name to **demo**, select the **test-project** log project, select the **test-logstore** Logstore, role operation to Create new role, and system policies to AliyunOSSFullAccess and AliyunLogFullAccess.

- See topic [Function operations](https://www.alibabacloud.com/help/doc-detail/73338.htm) to
    create a function.

    In this example, we select the **Empty Function** template, create no triggers, set the function name to **resize**, runtime environment to **Python**, and leave other parameters to their default values.

- Edit your function code.

### Create Trigger

- Log on to the [Function Compute console](https://fc.console.aliyun.com/).

- Click **Triggers** on the code execution page.

- Set the trigger type as **Object Storage Service (OSS)**, and select the new
bucket.

- Select oss:objectCreated:\* as the trigger event, and **source/** as the
prefix.

- In Invocation Role Management, select **Select an existing role**. The
system provides a role named AliyunOSSEventNotificationRole for the OSS trigger,
and you can select this role directly as the trigger role.

The trigger needs to set a trigger role to authorize the execution of the
function. OSS needs to play this role to trigger the function. For more
information on permissions, see [User permissions](https://www.alibabacloud.com/help/doc-detail/52885.htm).

After the OSS trigger is set, you can test the entire project. You can upload a
new image to the corresponding source/ directory in the Bucket in OSS console,
and you find a new resized image of the same name in the processed/ directory.

### Java For Function Compute

Function Compute currently supports OpenJDK1.8.0(runtime=java8).

When using Function Compute in Java, a class must be defined and a pre-defined
Function Compute interface must be implemented. A simplest function is defined
as follows:
```java
public class PgFunctionEntry implements StreamRequestHandler {
     /**
      * this is the entrance of FunctionCompute
      *
      * @param inputStream
      * @param outputStream
      * @param context
      * @throws IOException
      */
     @Override
     public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
         String inputStr = IOUtils.toString(inputStream, "utf-8");
         List<OssObjectContextDto> ossObjectContextDtoList = operateOssObject(inputStr, context);
         String csvFilePath = OssService.csvFileDownloadtoLocal(ossObjectContextDtoList);
         boolean isSuccess = CsvFileService.read(csvFilePath, ossObjectContextDtoList);
         if (!isSuccess) {//retry 3 times
             throw new IOException();
         }
         outputStream.write(new String(String.valueOf(isSuccess)).getBytes());
     }
}
```
-   Package name/Class name

    A package and a class can have random names, but their names must correspond to
the “handler” field of the created function. In the previous example, the
package name is “example” and the class name is “HelloFC”. Therefore, the
handler specified during function creation is example.HelloFC::handleRequest.
The format of “handler” is {package}.{class}::{method}.

-   Implement Interface

    The pre-defined Function Compute interface must be implemented in your code. In
the previous example, StreamRequestHandler is implemented, inputStream is the
data imported when the function is called, and outputStream is used to return
the function execution result. For more information about the function
interfaces, see **Function Interfaces**.

-   Context Parameter

    The context parameter contains the operation information of the function (such
as the request ID and temporary AccessKey). The parameter type
is com.aliyun.fc.runtime.Context.

-   InputStream

    For Oss trigger, the InputStream String will be like this below, and you can
process it by FastJson.
```java
{
    "events":[
        {
            "eventName":"ObjectCreated:PutObject",
            "eventSource":"acs:oss",
            "eventTime":"2017-04-21T12:46:37.000Z",
            "eventVersion":"1.0",
            "oss":{
                "bucket":{
                    "arn":"acs:oss:cn-shanghai:1237050315505689:bucketname",
                    "name":"bucketname",
                    "ownerIdentity":"1237050315505689",
                    "virtualBucket":""
                },
                "object":{
                    "deltaSize":122539,
                    "eTag":"688A7BF4F233DC9C88A80BF985AB7329",
                    "key":"image/a.jpg",
                    "size":122539
                },
                "ossSchemaVersion":"1.0",
                "ruleId":"9adac8e253828f4f7c0466d941fa3db81161e853"
            },
            "region":"cn-shanghai",
            "requestParameters":{
                "sourceIPAddress":"140.205.128.221"
            },
            "responseElements":{
                "requestId":"58F9FF2D3DF792092E12044C"
            },
            "userIdentity":{
                "principalId":"262561392693583141"
            }
        }
    ]
}
```

-   Return value

    The function that implements the StreamRequestHandler interface returns
execution results by using the outputStream parameter.

    The dependency of the com.aliyun.fc.runtime package can be referenced in the
following pom.xml:
```java
<dependencies>
   <dependency>
      <groupId>com.aliyun.fc.runtime</groupId>
      <artifactId>fc-java-core</artifactId>
      <version>1.0.0</version>
   </dependency>
   <dependency>
      <groupId>com.aliyun.oss</groupId>
      <artifactId>aliyun-sdk-oss</artifactId>
      <version>2.6.1</version>
   </dependency>
</dependencies>
```
In addition to that, you should add maven-assembly-plugin to pom.xml in order to
run mvn clean package –Dmaven.test.skip to package the dependencies.

### Function Interfaces

When you use the Java programming, a class must be implemented, which must
implement a pre-defined Function Compute interface. Currently, two pre-defined
interfaces can be implemented:

-   StreamRequestHandler

    This interface uses the stream request handler to receive the information
(events) input when calling a function and return execution results. You need to
read the input information from inputStream and to write the function execution
result into outputStream after the read operation is completed. The first
example in this document uses this interface.

-   PojoRequestHandler<I,O\>

    This interface uses the generic method to allow you to customize the input and
output types, but note that the types must be POJO.

If you want to see more details you can visit this address:
<https://www.alibabacloud.com/help/doc-detail/58887.htm>

## Code Practice

### Code Flow

![](images/e19638c72ceab9b287c41fdbbe755efd.png)

You can start to learn code by **PgFunctionEntry.handleRequest**.


### Use JDBC Operate HybridDB for Postgrey

You can use jdbc operate hybridDB for postgrey, for instance, you can read
DatabaseUtil class to set up connection. The class tells you how to create
database pool by druid.The key point is if the table is only for insert, then
you should assemble batch insert sql like: **insert into tableName(col1,col2….) values(val11,val21….),(val12,val22…),(val13,val23…);** if the table will execute
both insert and update, you can use batch upsert, the detail introduce page is:

gp_upsert:<https://github.com/digoal/blog/blob/master/201806/20180604_01.md>

gp_upsert_batch:<https://github.com/digoal/blog/blob/master/201806/20180605_01.md>

After you read the documents above, you will know if you want to use upsert, you
should create gp\_upsert and gp\_upsert\_batch functions first. The batch upsert sql will
be like this:

```sql
select gp_upsert_batch('staging', 'originreference', array['_row_id'],
array['{"_row_id":"cac45724-3e19-47ec-a848-265ad45538a4","originreferenceid":"21939576","paymentid":"342332","tdate":"20180123","tnum":"269873","createdby":"10942","createddate":"2018-01-23
00:00:00","tstamp":null,"eff_ts":"2018-01-23 17:57:01.866","end_ts":"9999-12-31
00:00:00","lock_version":"0","created_by":"storm@storm.com.au[Thread-11-SoftixBatchingRedshiftBolt-1.5.66.9]","created_date":"2018-01-23
17:57:34.157","updated_by":"storm@storm.com.au[Thread-11-SoftixBatchingRedshiftBolt-1.5.66.9]","updated_date":"2018-01-23
17:57:34.157"}'::json]);
```

The code detail you can read from **DbService.java**

## Support

If you need help, please contact [Zouping](zouping.lxp@alibaba-inc.com).