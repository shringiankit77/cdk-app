package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.*;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class NewStackBuildStack extends Stack {
    private String codeBuild = "DockerImageBuildAndPush";
    private String pipelineName = "codePipeline";

    public NewStackBuildStack(final Construct scope, final String id) {
        this(scope, id, null,null);
    }

    public NewStackBuildStack(final Construct scope, final String id, final StackProps props, final DataBaseandECSStack service) {
        super(scope, id, props);

        IRepository repository=   Repository.fromRepositoryName(this,"reopsitory","springboot-application-demo");
//        IRepository repository= Repository.fromRepositoryArn(this,"repository","")


        CodePipelineSource sourceCdk = CodePipelineSource.connection(
                "shringiankit77/cdk-app", "master",
                ConnectionSourceOptions.builder().connectionArn("arn:aws:codeconnections:ap-south-1:292659698864:connection/41693b7a-64b6-42f4-a0e0-889e7863e7f8").build()
        );
        CodePipelineSource source = CodePipelineSource.connection(
                "shringiankit77/first-java-app", "master",
                ConnectionSourceOptions.builder().connectionArn("arn:aws:codeconnections:ap-south-1:292659698864:connection/41693b7a-64b6-42f4-a0e0-889e7863e7f8").build()
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


        CodePipeline pipeline = CodePipeline.Builder.create(this, "SpringBootPipeline")
                .pipelineName("SpringBootJibPipeline")

                .synth(ShellStep.Builder.create("Synth")
                        .input(sourceCdk)

                       .commands(List.of(
                                "npm install -g aws-cdk",     // install CDK CLI
                                 "cdk synth"))
                        .build())
                .build();

        CodeBuildStep jibBuildStep = CodeBuildStep.Builder.create("JibBuildPush")
                .commands(List.of(
                        "echo Building and pushing image with Jib...",
                        "chmod +x gradlew",
                        "./gradlew jib --image=" + repository.getRepositoryUri()
                ))
                .input(source)
                .env(Map.of("IMAGE_NAME", repository.getRepositoryUri()))
                .rolePolicyStatements(ecrPolicyStatements)
                .build();



        StageDeployment buildStage = pipeline.addStage(new MyEcsDeployStage(this,"BuildDockerImage"), AddStageOpts.builder().build());
        buildStage.addPost(jibBuildStep);


        pipeline.buildPipeline();

        repository.grantPullPush(jibBuildStep.getProject());




    }
}
