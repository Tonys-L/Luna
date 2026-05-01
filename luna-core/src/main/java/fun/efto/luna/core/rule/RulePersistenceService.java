/**
 * 规则持久化服务
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.rule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.List;

public class RulePersistenceService {
    private static final String RULES_FILE = "luna-rules.json";

    /**
     * 保存规则到文件
     */
    public void saveRules() throws IOException {
        List<InjectionRule> rules = RuleManager.getInstance().getRules();
        String json = JSON.toJSONString(rules);
        try (FileWriter writer = new FileWriter(RULES_FILE)) {
            writer.write(json);
        }
    }

    /**
     * 从文件加载规则
     */
    public void loadRules() throws IOException {
        File file = new File(RULES_FILE);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            List<InjectionRule> rules = JSON.parseArray(json, InjectionRule.class);
            for (InjectionRule rule : rules) {
                RuleManager.getInstance().addRule(rule);
            }
        }
    }

    /**
     * 初始化规则持久化服务
     */
    public void initialize() {
        try {
            loadRules();
        } catch (IOException e) {
            System.err.println("加载规则失败: " + e.getMessage());
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
