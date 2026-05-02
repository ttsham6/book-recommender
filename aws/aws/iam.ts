import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

export interface IamArgs {
  modelBucketArn: pulumi.Output<string>;
  dynamoDbTableArn: pulumi.Output<string>;
}

export class Roles extends pulumi.ComponentResource {
  public readonly taskExecutionRoleArn: pulumi.Output<string>;
  public readonly taskRoleArn: pulumi.Output<string>;

  constructor(
    appName: string,
    args: IamArgs,
    opts?: pulumi.ComponentResourceOptions
  ) {
    super("custom:resource:iam-roles", appName, args, opts);

    const executionRole = new aws.iam.Role(
      `${appName}-execution-role`,
      {
        assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({
          Service: "ecs-tasks.amazonaws.com",
        }),
        managedPolicyArns: [
          "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy",
        ],
      },
      { parent: this }
    );

    const taskRole = new aws.iam.Role(
      `${appName}-task-role`,
      {
        assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({
          Service: "ecs-tasks.amazonaws.com",
        }),
      },
      { parent: this }
    );

    new aws.iam.RolePolicy(
      `${appName}-model-s3-policy`,
      {
        role: taskRole.name,
        policy: args.modelBucketArn.apply((arn) =>
          JSON.stringify({
            Version: "2012-10-17",
            Statement: [
              {
                Effect: "Allow",
                Action: ["s3:GetObject", "s3:ListBucket"],
                Resource: [arn, `${arn}/*`],
              },
            ],
          })
        ),
      },
      { parent: this }
    );

    new aws.iam.RolePolicy(
      `${appName}-dynamodb-policy`,
      {
        role: taskRole.name,
        policy: args.dynamoDbTableArn.apply((arn) =>
          JSON.stringify({
            Version: "2012-10-17",
            Statement: [
              {
                Effect: "Allow",
                Action: [
                  "dynamodb:BatchGetItem",
                  "dynamodb:BatchWriteItem",
                  "dynamodb:CreateTable",
                  "dynamodb:DeleteItem",
                  "dynamodb:DeleteTable",
                  "dynamodb:DescribeTable",
                  "dynamodb:GetItem",
                  "dynamodb:PutItem",
                  "dynamodb:Query",
                  "dynamodb:Scan",
                  "dynamodb:UpdateItem",
                ],
                Resource: arn,
              },
            ],
          })
        ),
      },
      { parent: this }
    );

    this.taskExecutionRoleArn = executionRole.arn;
    this.taskRoleArn = taskRole.arn;
  }
}
