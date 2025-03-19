package com.lws.jmeter.repository;

import com.lws.jmeter.entity.ScriptRunHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScriptRunHistoryRepository extends JpaRepository<ScriptRunHistory, Long> {

    List<ScriptRunHistory> findByScriptId(Long scriptId);
    @Modifying
    @Query("delete from ScriptRunHistory t where t.scriptId=:scriptId")
    void deleteByScriptId(@Param("scriptId") Long scriptId);

    @Modifying
    @Query("SELECT t from ScriptRunHistory t where t.scriptId=:scriptId and t.endTime is not null")
    List<ScriptRunHistory> getRunHistoryByScriptId(@Param("scriptId") Long scriptId);
}