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

// 模拟反编译代码数据
const mockDecompiledCode = {
  "com.example.MyController": `package com.example;

import com.example.model.User;
import com.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class MyController {
    
    private static final Logger logger = LoggerFactory.getLogger(MyController.class);
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        logger.info("Getting user with id: {}", id);
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        logger.info("Creating user: {}", user.getName());
        return userService.save(user);
    }
}`,
  "com.example.service.UserService": `package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserServiceInterface {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User findById(String id) {
        return userRepository.findById(id);
    }
    
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}`,
  "java.lang.String": `package java.lang;

import java.io.Serializable;
import java.util.Comparator;

public final class String
    implements Serializable, Comparable<String>, CharSequence {
    
    private final char[] value;
    private int hash;
    
    public String() {
        this.value = new char[0];
    }
    
    public String(String original) {
        this.value = original.value;
        this.hash = original.hash;
    }
    
    public int length() {
        return value.length;
    }
    
    public char charAt(int index) {
        if ((index < 0) || (index >= value.length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }
    
    // ... 其他方法
}`
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
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 800));
  
  // 如果找不到对应反编译代码，返回一个默认提示
  if (!mockDecompiledCode[className]) {
    return `// 无法获取类 ${className} 的反编译代码
// 该类可能是系统类或无法访问的类`;
  }
  
  return mockDecompiledCode[className];
}