package com.kingdee.qa.web;

import com.kingdee.qa.agent.Agent;
import com.kingdee.qa.config.ConfigManager;
import com.kingdee.qa.skill.KingdeeApiTool;
import com.kingdee.qa.skill.SkillLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话存储（线程安全）
 *
 * 管理所有 ChatSession 和对应的 Agent 实例。
 * 注册为 Spring @Component（单例），由 ChatController 注入使用。
 */
@Component
public class SessionStore {

    private final Map<String, Agent>       agents   = new ConcurrentHashMap<>();
    private final Map<String, ChatSession> sessions = Collections.synchronizedMap(new LinkedHashMap<>());

    private final SkillLoader    skillLoader;
    private final KingdeeApiTool apiTool;
    private final ConfigManager  config;

    public SessionStore(SkillLoader skillLoader,
                        KingdeeApiTool apiTool,
                        ConfigManager config) {
        this.skillLoader = skillLoader;
        this.apiTool     = apiTool;
        this.config      = config;
    }

    /** 创建新会话，返回会话 ID */
    public String createSession() {
        ChatSession session = new ChatSession();
        sessions.put(session.getId(), session);
        agents.put(session.getId(), new Agent(skillLoader, apiTool, config));
        return session.getId();
    }

    public ChatSession getSession(String id) {
        return sessions.get(id);
    }

    public Agent getAgent(String id) {
        return agents.get(id);
    }

    public void removeSession(String id) {
        sessions.remove(id);
        agents.remove(id);
    }

    /** 返回所有会话列表（最新在前） */
    public List<ChatSession> listSessions() {
        List<ChatSession> list = new ArrayList<>(sessions.values());
        Collections.reverse(list);
        return list;
    }

    public boolean exists(String id) {
        return sessions.containsKey(id);
    }
}
