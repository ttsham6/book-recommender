import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

export class ModelS3Bucket extends pulumi.ComponentResource {
  public readonly bucketName: pulumi.Output<string>;
  public readonly bucketArn: pulumi.Output<string>;

  constructor(name: string, opts?: pulumi.ComponentResourceOptions) {
    super("custom:resource:ModelS3Bucket", name, {}, opts);

    const bucket = new aws.s3.Bucket(
      `${name}-bucket`,
      {
        bucket: "bookreco-model-bucket",
        forceDestroy: true,
      },
      { parent: this }
    );

    this.bucketName = bucket.id;
    this.bucketArn = bucket.arn;
  }
}
