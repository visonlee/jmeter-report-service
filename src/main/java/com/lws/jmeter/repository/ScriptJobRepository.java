package com.lws.jmeter.repository;

import com.lws.jmeter.entity.ScriptJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScriptJobRepository extends JpaRepository<ScriptJob, Long> {

    @Query(value = "select * from script_job where expected_start_time <= now() order by expected_start_time desc limit 5", nativeQuery = true)
    List<ScriptJob> fetchPendingScriptJobs();
    ScriptJob findByScriptId(Long scriptId);

    @Modifying
    @Query("delete from ScriptJob t where t.scriptId=:scriptId")
    void deleteByScriptId(@Param("scriptId") Long scriptId);

}