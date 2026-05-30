package fun.efto.luna.core.rule;

import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则持久化服务
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class RulePersistenceService {
    private static final String RULES_FILE = "luna-rules.json";

    /**
     * 保存规则到文件
     */
    public void saveRules() throws IOException {
        List<InjectionRule> rules = RuleManager.getInstance().getRules();
        String json = JSON.toJSONString(rules);
        try (FileOutputStream fos = new FileOutputStream(RULES_FILE);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    /**
     * 从文件加载规则
     */
    public List<InjectionRule> loadRules() throws IOException {
        File file = new File(RULES_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return JSON.parseArray(json, InjectionRule.class);
        }
    }

    /**
     * 初始化规则持久化服务
     */
    public List<InjectionRule> initialize() {
        try {
            return loadRules();
        } catch (IOException e) {
            System.err.println("加载规则失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 关闭规则持久化服务
     */
    public void shutdown() {
        try {
            saveRules();
        } catch (IOException e) {
            System.err.println("保存规则失败: " + e.getMessage());
        }
    }
}
