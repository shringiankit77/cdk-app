package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.*;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.events.Connection;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class NewCdkSetUpStack extends Stack {
    private String codeBuild = "DockerImageBuildAndPush";
    private String pipelineName = "codePipeline";

    public NewCdkSetUpStack(final Construct scope, final String id) {
        this(scope, id, null,null);
    }

    public NewCdkSetUpStack(final Construct scope, final String id, final StackProps props, final DataBaseandECSStack service) {
        super(scope, id, props);

        IRepository repository=   Repository.fromRepositoryName(this,"reopsitory","springboot-application-demo");
//        IRepository repository= Repository.fromRepositoryArn(this,"repository","")


        CodePipelineSource sourceCdk = CodePipelineSource.connection(
                "shringiankit77/cdk-app", "master",
                ConnectionSourceOptions.builder().connectionArn(System.getenv("CONNECTION_URL_CDK")).build()
        );
        CodePipelineSource source = CodePipelineSource.connection(
                "shringiankit77/first-java-app", "master",
                ConnectionSourceOptions.builder().connectionArn(System.getenv("CONNECTION_URL")).build()
        );


        List<PolicyStatement> ecrPolicyStatements = List.of(
                PolicyStatement.Builder.create()
                        .actions(List.of("ecr:GetAuthorizationToken"))
                        .resources(List.of("*"))
                        .build(),
                PolicyStatement.Builder.create()
                        .actions(List.of(
                                "ecr:BatchCheckLayerAvailability",
                                "ecr:GetDownloadUrlForLayer",
                                "ecr:BatchGetImage",
                                "ecr:PutImage",
                                "ecr:InitiateLayerUpload",
                                "ecr:UploadLayerPart",
                                "ecr:CompleteLayerUpload"
                        ))
                        .resources(List.of(repository.getRepositoryArn()))
                        .build()
        );

        CodeBuildStep jibBuild = CodeBuildStep.Builder.create(codeBuild)
                .input(source)
                .env(Map.of("IMAGE_NAME", repository.getRepositoryUri()))
                .installCommands(List.of(
                        "chmod +x ./gradlew"
                ))
                .commands(List.of(
                        "export IMAGE_NAME=" + repository.getRepositoryUri(),
                        "./gradlew jib --image="+repository.getRepositoryUri()
                )).rolePolicyStatements(ecrPolicyStatements )

                .buildEnvironment(BuildEnvironment.builder()
                        .buildImage(LinuxBuildImage.STANDARD_7_0)
                        .privileged(true) // Required if using Docker
                        .build())
                .build();


        CodePipeline pipeline = CodePipeline.Builder.create(this, pipelineName)
                .pipelineName(pipelineName)
                .synth(jibBuild) // Can also use synth with CDK project if needed
                .build();
//        pipeline.addStage(Stage.of(service.getFargateService()));
        pipeline.buildPipeline();

        repository.grantPullPush(jibBuild.getProject());




    }
}
