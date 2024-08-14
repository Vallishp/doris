// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.expressions.functions.scalar;

import org.apache.doris.catalog.FunctionSignature;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.functions.ExplicitlyCastableSignature;
import org.apache.doris.nereids.trees.expressions.functions.PropagateNullable;
import org.apache.doris.nereids.trees.expressions.visitor.ExpressionVisitor;
import org.apache.doris.nereids.types.BigIntType;
import org.apache.doris.nereids.types.DataType;
import org.apache.doris.nereids.types.DateTimeType;
import org.apache.doris.nereids.types.DateTimeV2Type;
import org.apache.doris.nereids.types.DateType;
import org.apache.doris.nereids.types.DateV2Type;
import org.apache.doris.nereids.types.DecimalV2Type;
import org.apache.doris.nereids.types.DecimalV3Type;
import org.apache.doris.nereids.types.DoubleType;
import org.apache.doris.nereids.types.FloatType;
import org.apache.doris.nereids.types.IntegerType;
import org.apache.doris.nereids.types.LargeIntType;
import org.apache.doris.nereids.types.SmallIntType;
import org.apache.doris.nereids.types.StringType;
import org.apache.doris.nereids.types.TinyIntType;
import org.apache.doris.nereids.types.VarcharType;
import org.apache.doris.nereids.types.coercion.CharacterType;
import org.apache.doris.nereids.util.ExpressionUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * ScalarFunction 'least'. This class is generated by GenerateFunction.
 */
public class Least extends ScalarFunction
        implements ExplicitlyCastableSignature, PropagateNullable {

    public static final List<FunctionSignature> SIGNATURES = ImmutableList.of(
            FunctionSignature.ret(VarcharType.SYSTEM_DEFAULT).varArgs(VarcharType.SYSTEM_DEFAULT),
            FunctionSignature.ret(StringType.INSTANCE).varArgs(StringType.INSTANCE),
            FunctionSignature.ret(DateTimeV2Type.SYSTEM_DEFAULT).varArgs(DateTimeV2Type.SYSTEM_DEFAULT),
            FunctionSignature.ret(DateTimeType.INSTANCE).varArgs(DateTimeType.INSTANCE),
            FunctionSignature.ret(DateV2Type.INSTANCE).varArgs(DateV2Type.INSTANCE),
            FunctionSignature.ret(DateType.INSTANCE).varArgs(DateType.INSTANCE),
            FunctionSignature.ret(DoubleType.INSTANCE).varArgs(DoubleType.INSTANCE),
            FunctionSignature.ret(FloatType.INSTANCE).varArgs(FloatType.INSTANCE),
            FunctionSignature.ret(DecimalV3Type.WILDCARD).varArgs(DecimalV3Type.WILDCARD),
            FunctionSignature.ret(DecimalV2Type.SYSTEM_DEFAULT).varArgs(DecimalV2Type.SYSTEM_DEFAULT),
            FunctionSignature.ret(LargeIntType.INSTANCE).varArgs(LargeIntType.INSTANCE),
            FunctionSignature.ret(BigIntType.INSTANCE).varArgs(BigIntType.INSTANCE),
            FunctionSignature.ret(IntegerType.INSTANCE).varArgs(IntegerType.INSTANCE),
            FunctionSignature.ret(SmallIntType.INSTANCE).varArgs(SmallIntType.INSTANCE),
            FunctionSignature.ret(TinyIntType.INSTANCE).varArgs(TinyIntType.INSTANCE)
    );

    /**
     * constructor with 1 or more arguments.
     */
    public Least(Expression arg, Expression... varArgs) {
        super("least", ExpressionUtils.mergeArguments(arg, varArgs));
    }

    /**
     * withChildren.
     */
    @Override
    public Least withChildren(List<Expression> children) {
        Preconditions.checkArgument(!children.isEmpty());
        return new Least(children.get(0),
                children.subList(1, children.size()).toArray(new Expression[0]));
    }

    @Override
    public FunctionSignature searchSignature(List<FunctionSignature> signatures) {
        List<DataType> argTypes = getArgumentsTypes();
        if (argTypes.stream().anyMatch(CharacterType.class::isInstance)) {
            return FunctionSignature.ret(StringType.INSTANCE).varArgs(StringType.INSTANCE);
        } else if (argTypes.stream().anyMatch(DateTimeV2Type.class::isInstance)) {
            return FunctionSignature.ret(DateTimeV2Type.SYSTEM_DEFAULT).varArgs(DateTimeV2Type.SYSTEM_DEFAULT);
        } else if (argTypes.stream().anyMatch(DateTimeType.class::isInstance)) {
            return FunctionSignature.ret(DateTimeType.INSTANCE).varArgs(DateTimeType.INSTANCE);
        } else if (argTypes.stream().anyMatch(DateV2Type.class::isInstance)) {
            return FunctionSignature.ret(DateV2Type.INSTANCE).varArgs(DateV2Type.INSTANCE);
        } else if (argTypes.stream().anyMatch(DateType.class::isInstance)) {
            return FunctionSignature.ret(DateType.INSTANCE).varArgs(DateType.INSTANCE);
        }
        return ExplicitlyCastableSignature.super.searchSignature(signatures);
    }

    @Override
    public List<FunctionSignature> getSignatures() {
        return SIGNATURES;
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitLeast(this, context);
    }
}
