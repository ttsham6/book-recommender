import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

export class DynamoDB extends pulumi.ComponentResource {
  public readonly tableName: pulumi.Output<string>;
  public readonly tableArn: pulumi.Output<string>;

  constructor(name: string, opts?: pulumi.ComponentResourceOptions) {
    super("custom:resource:DynamoDB", name, {}, opts);

    const table = new aws.dynamodb.Table(
      `${name}-table`,
      {
        name: "book",
        attributes: [{ name: "id", type: "S" }],
        hashKey: "id",
        billingMode: "PAY_PER_REQUEST",
        pointInTimeRecovery: {
          enabled: true,
        },
      },
      { parent: this }
    );

    this.tableName = table.name;
    this.tableArn = table.arn;
  }
}
