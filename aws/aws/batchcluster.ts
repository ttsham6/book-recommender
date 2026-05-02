import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as pulumi from "@pulumi/pulumi";

export interface BatchArgs {
  subnetIds: pulumi.Output<string[]>;
  containerSgId: pulumi.Output<string>;
  taskRoleArn: pulumi.Output<string>;
  executionRoleArn: pulumi.Output<string>;
  contextPath: string;
  environments: { name: string; value: pulumi.Input<string> }[];
  scheduleExpression?: string;
  imageTag?: string;
}

export class BatchService extends pulumi.ComponentResource {
  constructor(
    appName: string,
    args: BatchArgs,
    opts?: pulumi.ComponentResourceOptions
  ) {
    super(`custom:resource:${appName}-batch`, appName, args, opts);

    const cluster = new aws.ecs.Cluster(
      `${appName}-cluster`,
      {},
      { parent: this }
    );

    const repo = new awsx.ecr.Repository(
      `${appName}-repo`,
      {},
      { parent: this }
    );

    const image = new awsx.ecr.Image(
      `${appName}-image`,
      {
        repositoryUrl: repo.url,
        context: args.contextPath,
        imageTag: args.imageTag || "latest",
        platform: "linux/amd64",
      },
      { parent: this }
    );

    const logGroup = new aws.cloudwatch.LogGroup(
      `${appName}-log-group`,
      {
        name: `/ecs/${appName}-batch`,
        retentionInDays: 7,
      },
      { parent: this }
    );

    const taskDefinition = new aws.ecs.TaskDefinition(
      `${appName}-task`,
      {
        family: `${appName}-task`,
        cpu: "256",
        memory: "2048",
        requiresCompatibilities: ["FARGATE"],
        networkMode: "awsvpc",
        taskRoleArn: args.taskRoleArn,
        executionRoleArn: args.executionRoleArn,
        containerDefinitions: pulumi
          .all([image.imageUri, args.environments, logGroup.name])
          .apply(([imageUri, envs, logGroupName]) =>
            JSON.stringify([
              {
                name: "batch-container",
                image: imageUri,
                essential: true,
                environment: envs.concat([
                  {
                    name: "JAVA_OPTS",
                    value:
                      "-Xmx1536m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof",
                  },
                ]),
                logConfiguration: {
                  logDriver: "awslogs",
                  options: {
                    "awslogs-group": logGroupName,
                    "awslogs-region": "ap-northeast-1",
                    "awslogs-stream-prefix": "ecs",
                  },
                },
              },
            ])
          ),
      },
      { parent: this }
    );

    const eventBridgeRole = new aws.iam.Role(
      `${appName}-events-role`,
      {
        assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({
          Service: "events.amazonaws.com",
        }),
      },
      { parent: this }
    );

    new aws.iam.RolePolicy(
      `${appName}-events-policy`,
      {
        role: eventBridgeRole.name,
        policy: pulumi
          .all([
            taskDefinition.arn,
            args.executionRoleArn,
            args.taskRoleArn,
            cluster.arn,
          ])
          .apply(([taskArn, executionRoleArn, taskRoleArn, clusterArn]) =>
            JSON.stringify({
              Version: "2012-10-17",
              Statement: [
                {
                  Effect: "Allow",
                  Action: "ecs:RunTask",
                  Resource: taskArn,
                  Condition: {
                    ArnEquals: {
                      "ecs:cluster": clusterArn,
                    },
                  },
                },
                {
                  Effect: "Allow",
                  Action: "iam:PassRole",
                  Resource: [executionRoleArn, taskRoleArn],
                },
              ],
            })
          ),
      },
      { parent: this }
    );

    const rule = new aws.cloudwatch.EventRule(
      `${appName}-rule`,
      {
        description: `Schedule for ${appName} batch`,
        scheduleExpression: args.scheduleExpression || "cron(0 15 * * ? *)",
      },
      { parent: this }
    );

    new aws.cloudwatch.EventTarget(
      `${appName}-target`,
      {
        rule: rule.name,
        arn: cluster.arn,
        roleArn: eventBridgeRole.arn,
        ecsTarget: {
          taskDefinitionArn: taskDefinition.arn,
          launchType: "FARGATE",
          platformVersion: "LATEST",
          networkConfiguration: {
            subnets: args.subnetIds,
            securityGroups: [args.containerSgId],
            assignPublicIp: true,
          },
        },
      },
      { parent: this }
    );
  }
}
