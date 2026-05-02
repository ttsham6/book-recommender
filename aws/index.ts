import * as pulumi from "@pulumi/pulumi";
import * as apicluster from "./aws/apicluster";
import * as apigateway from "./aws/apigateway";
import * as batchcluster from "./aws/batchcluster";
import * as dynamodb from "./aws/dynamodb";
import * as iam from "./aws/iam";
import * as network from "./aws/network";
import * as s3 from "./aws/s3";

const config = new pulumi.Config();
const prefix = "bookreco";

const environment = config.get("environment") || "shared,shared-prod,prod";
const pineconeIndex = config.get("pineconeIndex") || "book-index";

const vpc = new network.Vpc(`${prefix}-vpc`);
const modelS3 = new s3.ModelS3Bucket(`${prefix}-model`);
const bookTable = new dynamodb.DynamoDB(`${prefix}-dynamodb`);

const roles = new iam.Roles(prefix, {
  modelBucketArn: modelS3.bucketArn,
  dynamoDbTableArn: bookTable.tableArn,
});

const commonEnvs = [
  { name: "SPRING_PROFILES_ACTIVE", value: environment },
  { name: "S3_MODEL_BUCKET", value: modelS3.bucketName },
  { name: "S3_MODEL_PREFIX", value: "model/" },
  { name: "MODEL_DIR", value: "/tmp/model" },
  { name: "BOOK_RECOMMENDER_PINECONE_INDEX", value: pineconeIndex },
];

const secretEnvs = [
  { name: "RAKUTEN_APPLICATION_ID", value: config.requireSecret("rakutenApplicationId") },
  { name: "RAKUTEN_ACCESS_KEY", value: config.requireSecret("rakutenAccessKey") },
  { name: "PINECONE_API_KEY", value: config.requireSecret("pineconeApiKey") },
];

new batchcluster.BatchService(`${prefix}-batch`, {
  subnetIds: vpc.ecsSubnetIds,
  containerSgId: vpc.batchSecurityGroupId,
  taskRoleArn: roles.taskRoleArn,
  executionRoleArn: roles.taskExecutionRoleArn,
  contextPath: "./app/batch",
  environments: [...commonEnvs, ...secretEnvs],
  scheduleExpression: "cron(0 15 * * ? *)",
});

const api = new apicluster.ApiService(`${prefix}-api`, {
  subnetIds: vpc.ecsSubnetIds,
  containerSgId: vpc.apiSecurityGroupId,
  taskRoleArn: roles.taskRoleArn,
  executionRoleArn: roles.taskExecutionRoleArn,
  contextPath: "./app/api",
  environments: [...commonEnvs, ...secretEnvs],
});

new apigateway.ApiGateway(`${prefix}-apigateway`, {
  lbDnsName: api.loadBalancerDnsName,
  subnetIds: vpc.ecsSubnetIds,
  securityGroupIds: [vpc.vpcLinkSecurityGroupId],
  listenerArn: api.loadBalancerListenerArn,
  serviceDnsName: "book-reco.com",
  certificateArn: config.get("certificateArn") || undefined,
  hostedZoneId: config.get("hostedZoneId") || undefined,
});

export const vpcId = vpc.vpcId;
export const ecsSubnetIds = vpc.ecsSubnetIds;
export const modelBucketName = modelS3.bucketName;
export const bookTableName = bookTable.tableName;
export const apiLoadBalancerDnsName = api.loadBalancerDnsName;
