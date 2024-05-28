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

#pragma once

#include <gen_cpp/Types_types.h>

#include <atomic>
#include <condition_variable>
#include <map>
#include <mutex>
#include <set>
#include <vector>

#include "common/status.h"
#include "olap/olap_common.h"
#include "olap/rowset/rowset_fwd.h"
#include "olap/tablet_fwd.h"
#include "olap/task/engine_task.h"
#include "runtime/memory/mem_tracker_limiter.h"
#include "util/time.h"

namespace doris {

class EnginePublishVersionTask;
class TPublishVersionRequest;
class StorageEngine;

struct TabletPublishStatistics {
    int64_t submit_time_us = 0;
    int64_t schedule_time_us = 0;
    int64_t lock_wait_time_us = 0;
    int64_t save_meta_time_us = 0;
    int64_t calc_delete_bitmap_time_us = 0;
    int64_t partial_update_write_segment_us = 0;
    int64_t add_inc_rowset_us = 0;

    std::string to_string() {
        return fmt::format(
                "[Publish Statistics: schedule time(us): {}, lock wait time(us): {}, save meta "
                "time(us): {}, calc delete bitmap time(us): {}, partial update write segment "
                "time(us): {}, add inc rowset time(us): {}]",
                schedule_time_us, lock_wait_time_us, save_meta_time_us, calc_delete_bitmap_time_us,
                partial_update_write_segment_us, add_inc_rowset_us);
    }

    void record_in_bvar();
};

class TabletPublishTxnTask {
public:
    TabletPublishTxnTask(StorageEngine& engine, EnginePublishVersionTask* engine_task,
                         TabletSharedPtr tablet, RowsetSharedPtr rowset, int64_t partition_id,
                         int64_t transaction_id, Version version, const TabletInfo& tablet_info);
    ~TabletPublishTxnTask();

    void handle();
    Status result() { return _result; }

private:
    StorageEngine& _engine;
    EnginePublishVersionTask* _engine_publish_version_task = nullptr;

    TabletSharedPtr _tablet;
    RowsetSharedPtr _rowset;
    int64_t _partition_id;
    int64_t _transaction_id;
    Version _version;
    TabletInfo _tablet_info;
    TabletPublishStatistics _stats;
    Status _result;
    std::shared_ptr<MemTrackerLimiter> _mem_tracker;
};

class EnginePublishVersionTask final : public EngineTask {
public:
    EnginePublishVersionTask(
            StorageEngine& engine, const TPublishVersionRequest& publish_version_req,
            std::set<TTabletId>* error_tablet_ids, std::map<TTabletId, TVersion>* succ_tablets,
            std::vector<std::tuple<int64_t, int64_t, int64_t>>* discontinous_version_tablets,
            std::map<TTableId, std::map<TTabletId, int64_t>>*
                    table_id_to_tablet_id_to_num_delta_rows);
    ~EnginePublishVersionTask() override = default;

    Status execute() override;

    void add_error_tablet_id(int64_t tablet_id);

private:
    void _calculate_tbl_num_delta_rows(
            const std::unordered_map<int64_t, int64_t>& tablet_id_to_num_delta_rows);

    StorageEngine& _engine;
    const TPublishVersionRequest& _publish_version_req;
    std::mutex _tablet_ids_mutex;
    std::set<TTabletId>* _error_tablet_ids = nullptr;
    std::map<TTabletId, TVersion>* _succ_tablets;
    std::vector<std::tuple<int64_t, int64_t, int64_t>>* _discontinuous_version_tablets = nullptr;
    std::map<TTableId, std::map<TTabletId, int64_t>>* _table_id_to_tablet_id_to_num_delta_rows =
            nullptr;
};

class AsyncTabletPublishTask {
public:
    AsyncTabletPublishTask(StorageEngine& engine, TabletSharedPtr tablet, int64_t partition_id,
                           int64_t transaction_id, int64_t version)
            : _engine(engine),
              _tablet(std::move(tablet)),
              _partition_id(partition_id),
              _transaction_id(transaction_id),
              _version(version),
              _mem_tracker(MemTrackerLimiter::create_shared(MemTrackerLimiter::Type::OTHER,
                                                            "AsyncTabletPublishTask")) {
        _stats.submit_time_us = MonotonicMicros();
    }
    ~AsyncTabletPublishTask() = default;

    void handle();

private:
    StorageEngine& _engine;
    TabletSharedPtr _tablet;
    int64_t _partition_id;
    int64_t _transaction_id;
    int64_t _version;
    TabletPublishStatistics _stats;
    std::shared_ptr<MemTrackerLimiter> _mem_tracker;
};

} // namespace doris
