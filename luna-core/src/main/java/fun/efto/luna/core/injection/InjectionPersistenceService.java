/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:55
 */
package fun.efto.luna.core.injection;

import com.alibaba.fastjson.JSON;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class InjectionPersistenceService {
    private static final String FILE_NAME = "luna-injections.json";

    public void save(List<PersistentInjection> injections) throws IOException {
        List<PersistentInjection> persistentOnly = new ArrayList<>();
        for (PersistentInjection inj : injections) {
            if (!inj.isEphemeral()) {
                persistentOnly.add(inj);
            }
        }
        String json = JSON.toJSONString(persistentOnly);
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    public List<PersistentInjection> load() throws IOException {
        File file = new File(FILE_NAME);
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
            List<PersistentInjection> list = JSON.parseArray(json, PersistentInjection.class);
            if (list == null) {
                return new ArrayList<>();
            }
            // Filter out old format records with null method field
            list.removeIf(inj -> {
                if (inj.getMethodName() == null && inj.getInjectionType() != null
                        && !inj.getInjectionType().contains("line")) {
                    System.err.println("[Luna] WARN: Skipping legacy injection record with null method: id="
                        + inj.getId() + ", type=" + inj.getInjectionType() + ", class=" + inj.getClazz());
                    return true;
                }
                return false;
            });
            return list;
        }
    }
}
