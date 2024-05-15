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

package org.apache.doris.nereids.trees.plans.logical;

import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.properties.ExprFdItem;
import org.apache.doris.nereids.properties.FdFactory;
import org.apache.doris.nereids.properties.FdItem;
import org.apache.doris.nereids.properties.FunctionalDependencies;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.Slot;
import org.apache.doris.nereids.trees.expressions.SlotReference;
import org.apache.doris.nereids.trees.plans.LimitPhase;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.algebra.Limit;
import org.apache.doris.nereids.trees.plans.visitor.PlanVisitor;
import org.apache.doris.nereids.util.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Logical limit plan
 * eg: select * from table limit 10
 * limit: 10
 * <p>
 * eg: select * from table order by a limit 100, 10
 * limit: 10
 * offset 100
 */
public class LogicalLimit<CHILD_TYPE extends Plan> extends LogicalUnary<CHILD_TYPE> implements Limit {
    private final LimitPhase phase;
    private final long limit;
    private final long offset;

    public LogicalLimit(long limit, long offset, LimitPhase phase, CHILD_TYPE child) {
        this(limit, offset, phase, Optional.empty(), Optional.empty(), child);
    }

    public LogicalLimit(long limit, long offset, LimitPhase phase, Optional<GroupExpression> groupExpression,
            Optional<LogicalProperties> logicalProperties, CHILD_TYPE child) {
        super(PlanType.LOGICAL_LIMIT, groupExpression, logicalProperties, child);
        this.limit = limit;
        this.offset = offset;
        this.phase = phase;
    }

    public LimitPhase getPhase() {
        return phase;
    }

    public boolean isSplit() {
        return phase != LimitPhase.ORIGIN;
    }

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public List<Slot> computeOutput() {
        return child().getOutput();
    }

    @Override
    public String toString() {
        return Utils.toSqlString("LogicalLimit",
                "limit", limit,
                "offset", offset,
                "phase", phase
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogicalLimit<?> that = (LogicalLimit<?>) o;
        return limit == that.limit && offset == that.offset && phase == that.phase;
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
        return visitor.visitLogicalLimit(this, context);
    }

    public List<? extends Expression> getExpressions() {
        return ImmutableList.of();
    }

    public LogicalLimit<Plan> withLimitChild(long limit, long offset, Plan child) {
        Preconditions.checkArgument(children.size() == 1,
                "LogicalTopN should have 1 child, but input is %s", children.size());
        return new LogicalLimit<>(limit, offset, phase, child);
    }

    @Override
    public Plan withGroupExpression(Optional<GroupExpression> groupExpression) {
        return new LogicalLimit<>(limit, offset, phase, groupExpression, Optional.of(getLogicalProperties()), child());
    }

    @Override
    public Plan withGroupExprLogicalPropChildren(Optional<GroupExpression> groupExpression,
            Optional<LogicalProperties> logicalProperties, List<Plan> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new LogicalLimit<>(limit, offset, phase, groupExpression, logicalProperties, children.get(0));
    }

    @Override
    public LogicalLimit<Plan> withChildren(List<Plan> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new LogicalLimit<>(limit, offset, phase, children.get(0));
    }

    @Override
    public void computeUnique(FunctionalDependencies.Builder fdBuilder) {
        if (getLimit() == 1) {
            getOutput().forEach(fdBuilder::addUniqueSlot);
        } else {
            fdBuilder.addUniqueSlot(child(0).getLogicalProperties().getFunctionalDependencies());
        }
    }

    @Override
    public void computeUniform(FunctionalDependencies.Builder fdBuilder) {
        if (getLimit() == 1) {
            getOutput().forEach(fdBuilder::addUniformSlot);
        } else {
            fdBuilder.addUniformSlot(child(0).getLogicalProperties().getFunctionalDependencies());
        }
    }

    @Override
    public ImmutableSet<FdItem> computeFdItems() {
        ImmutableSet<FdItem> fdItems = child(0).getLogicalProperties().getFunctionalDependencies().getFdItems();
        if (getLimit() == 1 && !phase.isLocal()) {
            ImmutableSet.Builder<FdItem> builder = ImmutableSet.builder();
            List<Slot> output = getOutput();
            ImmutableSet<SlotReference> slotSet = output.stream()
                    .filter(SlotReference.class::isInstance)
                    .map(SlotReference.class::cast)
                    .collect(ImmutableSet.toImmutableSet());
            ExprFdItem fdItem = FdFactory.INSTANCE.createExprFdItem(slotSet, true, slotSet);
            builder.add(fdItem);
            fdItems = builder.build();
        }
        return fdItems;
    }

    @Override
    public void computeEqualSet(FunctionalDependencies.Builder fdBuilder) {
        fdBuilder.addEqualSet(child().getLogicalProperties().getFunctionalDependencies());
    }

    @Override
    public void computeFd(FunctionalDependencies.Builder fdBuilder) {
        fdBuilder.addFuncDepsDG(child().getLogicalProperties().getFunctionalDependencies());
    }
}
