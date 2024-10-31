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

package org.apache.doris.nereids.trees.plans.commands;

import org.apache.doris.analysis.RecoverTableStmt;
import org.apache.doris.analysis.StmtType;
import org.apache.doris.analysis.TableName;
import org.apache.doris.catalog.Env;
import org.apache.doris.common.ErrorCode;
import org.apache.doris.common.ErrorReport;
import org.apache.doris.common.UserException;
import org.apache.doris.common.util.Util;
import org.apache.doris.mysql.privilege.PrivPredicate;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.commands.info.TableNameInfo;
import org.apache.doris.nereids.trees.plans.visitor.PlanVisitor;
import org.apache.doris.qe.ConnectContext;
import org.apache.doris.qe.StmtExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * recover table command
 */
public class RecoverTableCommand extends Command implements ForwardWithSync {

    public static final Logger LOG = LogManager.getLogger(RecoverTableCommand.class);
    private TableNameInfo dbTblName;
    private long tableId = -1;
    private String newTableName = "";

    /**
     * constructor
     */

    public RecoverTableCommand(TableNameInfo dbTblName, long tableId, String newTableName) {
        super(PlanType.RECOVER_TABLE_COMMAND);
        this.dbTblName = dbTblName;
        this.tableId = tableId;
        if (newTableName != null) {
            this.newTableName = newTableName;
        }
    }

    public String getDbName() {
        return dbTblName.getDb();
    }

    public String getTableName() {
        return dbTblName.getTbl();
    }

    public long getTableId() {
        return tableId;
    }

    public String getNewTableName() {
        return newTableName;
    }

    @Override
    public void run(ConnectContext ctx, StmtExecutor executor) throws UserException {
        // from connection need to update current catalog
        // and database.
        dbTblName.analyze(ctx);
        Util.prohibitExternalCatalog(dbTblName.getCtl(), this.getClass().getSimpleName());

        if (!Env.getCurrentEnv().getAccessManager().checkTblPriv(
                ConnectContext.get(), dbTblName.getCtl(), dbTblName.getDb(), dbTblName.getTbl(),
                PrivPredicate.ALTER_CREATE)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_TABLEACCESS_DENIED_ERROR, "RECOVERY",
                    ConnectContext.get().getQualifiedUser(),
                    ConnectContext.get().getRemoteIP(),
                    dbTblName.getDb() + ": " + dbTblName.getTbl());
        }
        TableName tableName = new TableName(dbTblName.getCtl(), dbTblName.getDb(), dbTblName.getTbl());
        RecoverTableStmt recoverTblStmt = new RecoverTableStmt(tableName, tableId, newTableName);
        Env.getCurrentEnv().recoverTable(recoverTblStmt);
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
        return visitor.visitRecoverTableCommand(this, context);
    }

    @Override
    public StmtType stmtType() {
        return StmtType.RECOVER;
    }
}
