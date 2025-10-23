// 模拟API调用，实际项目中需要替换为真实的后端API调用

// 模拟类列表数据
const mockClassList = {
  "jdk.internal.loader.ClassLoaders$AppClassLoader": [
    {"className": "com.alibaba.fastjson2.writer.FieldWriterEnum"},
    {"className": "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter"},
    {"className": "org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer$$Lambda$696/0x0000000801362408"},
    {"className": "com.example.MyController"},
    {"className": "com.example.service.UserService"}
  ],
  "jdk.internal.loader.ClassLoaders$PlatformClassLoader": [
    {"className": "java.sql.DriverManager"},
    {"className": "javax.xml.parsers.DocumentBuilderFactory"}
  ],
  "jdk.internal.loader.ClassLoaders$BootClassLoader": [
    {"className": "java.lang.Object"},
    {"className": "java.lang.String"},
    {"className": "java.lang.Integer"},
    {"className": "java.util.List"},
    {"className": "java.util.ArrayList"}
  ]
};

// 模拟类信息数据
const mockClassInfo = {
  "com.example.MyController": {
    "className": "com.example.MyController",
    "fields": [
      {"name": "userService", "descriptor": "Lcom/example/service/UserService;", "accessFlags": 2},
      {"name": "logger", "descriptor": "Lorg/slf4j/Logger;", "accessFlags": 10}
    ],
    "methods": [
      {"name": "<init>", "descriptor": "()V", "accessFlags": 1, "parameters": []},
      {"name": "getUser", "descriptor": "(Ljava/lang/String;)Lcom/example/model/User;", "accessFlags": 1, 
       "parameters": [{"name": "id", "descriptor": "Ljava/lang/String;"}]},
      {"name": "createUser", "descriptor": "(Lcom/example/model/User;)Lcom/example/model/User;", "accessFlags": 1,
       "parameters": [{"name": "user", "descriptor": "Lcom/example/model/User;"}]}
    ],
    "superClass": "java.lang.Object",
    "interfaces": [],
    "accessFlags": 33
  },
  "com.example.service.UserService": {
    "className": "com.example.service.UserService",
    "fields": [
      {"name": "userRepository", "descriptor": "Lcom/example/repository/UserRepository;", "accessFlags": 2}
    ],
    "methods": [
      {"name": "<init>", "descriptor": "()V", "accessFlags": 1, "parameters": []},
      {"name": "findById", "descriptor": "(Ljava/lang/String;)Lcom/example/model/User;", "accessFlags": 1,
       "parameters": [{"name": "id", "descriptor": "Ljava/lang/String;"}]},
      {"name": "save", "descriptor": "(Lcom/example/model/User;)Lcom/example/model/User;", "accessFlags": 1,
       "parameters": [{"name": "user", "descriptor": "Lcom/example/model/User;"}]}
    ],
    "superClass": "java.lang.Object",
    "interfaces": ["com.example.service.UserServiceInterface"],
    "accessFlags": 33
  },
  "java.lang.String": {
    "className": "java.lang.String",
    "fields": [
      {"name": "value", "descriptor": "[C", "accessFlags": 18},
      {"name": "hash", "descriptor": "I", "accessFlags": 18}
    ],
    "methods": [
      {"name": "<init>", "descriptor": "()V", "accessFlags": 1, "parameters": []},
      {"name": "<init>", "descriptor": "(Ljava/lang/String;)V", "accessFlags": 1,
       "parameters": [{"name": "original", "descriptor": "Ljava/lang/String;"}]},
      {"name": "length", "descriptor": "()I", "accessFlags": 17, "parameters": []},
      {"name": "charAt", "descriptor": "(I)C", "accessFlags": 17,
       "parameters": [{"name": "index", "descriptor": "I"}]}
    ],
    "superClass": "java.lang.Object",
    "interfaces": ["java.io.Serializable", "java.lang.Comparable", "java.lang.CharSequence"],
    "accessFlags": 49
  }
};

/**
 * 获取类列表
 * @returns {Promise<Object>} 类列表数据
 */
export async function getClassList() {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 500));
  return mockClassList;
}

/**
 * 获取类详细信息
 * @param {string} className 类名
 * @returns {Promise<Object>} 类详细信息
 */
export async function getClassInfo(className) {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 如果找不到对应类信息，返回一个默认结构
  if (!mockClassInfo[className]) {
    return {
      className: className,
      fields: [],
      methods: [],
      superClass: "java.lang.Object",
      interfaces: [],
      accessFlags: 1
    };
  }
  
  return mockClassInfo[className];
}

/**
 * 获取反编译代码
 * @param {string} className 类名
 * @returns {Promise<string>} 反编译代码
 */
export async function getDecompiledCode(className) {
  try {
    // 调用真实的后端API接口 /api/decompile?class=className
    const response = await fetch(`/api/decompile?class=${encodeURIComponent(className)}`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    // 获取响应JSON数据
    const result = await response.json();
    
    // 如果返回了错误信息，直接返回错误
    if (result.error) {
      return `// 反编译失败: ${result.error}\n// 类名: ${className}`;
    }
    
    // 返回反编译代码
    return result.decompiled || "// 未获取到反编译代码";
  } catch (error) {
    console.error('获取反编译代码失败:', error);
    return `// 获取反编译代码失败: ${error.message}\n// 类名: ${className}`;
  }
}