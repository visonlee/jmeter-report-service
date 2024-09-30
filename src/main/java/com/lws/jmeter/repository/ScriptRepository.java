package com.lws.jmeter.repository;

import com.lws.jmeter.entity.Script;
import com.lws.jmeter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptRepository extends JpaRepository<Script, Long> {

}