package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class StackImpl extends Stack {
    public StackImpl(final Construct scope, final String id){
        super(scope,id);
        StringParameter.Builder.create(this, "SelfMuteFlag")
                .parameterName("/selfmute/enabled")
                .stringValue("true")
                .build();
    }
}