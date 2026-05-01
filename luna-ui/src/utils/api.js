import {get, post} from './request';

// 模拟数据
const mockClassTree = {
  "Bootstrap ClassLoader": [
    { className: "java.lang.Object" },
    { className: "java.lang.String" },
    { className: "java.lang.Integer" },
    { className: "java.util.ArrayList" },
    { className: "java.util.HashMap" }
  ],
  "Application ClassLoader": [
    { className: "com.example.HelloWorld" },
    { className: "com.example.User" },
    { className: "com.example.service.UserService" },
    { className: "com.example.controller.UserController" },
    { className: "com.example.model.UserModel" }
  ],
  "Extension ClassLoader": [
    { className: "sun.misc.BASE64Encoder" },
    { className: "sun.misc.BASE64Decoder" }
  ]
};

const mockClassAnalysis = {
  className: "com.example.User",
  packageName: "com.example",
  superClass: "java.lang.Object",
  interfaces: [],
  fields: [
    { name: "id", type: "int", access: "private" },
    { name: "name", type: "java.lang.String", access: "private" },
    { name: "age", type: "int", access: "private" }
  ],
  methods: [
    { name: "<init>", descriptor: "()V", access: "public" },
    { name: "getId", descriptor: "()I", access: "public" },
    { name: "setId", descriptor: "(I)V", access: "public" },
    { name: "getName", descriptor: "()Ljava/lang/String;", access: "public" },
    { name: "setName", descriptor: "(Ljava/lang/String;)V", access: "public" },
    { name: "getAge", descriptor: "()I", access: "public" },
    { name: "setAge", descriptor: "(I)V", access: "public" }
  ]
};

const mockDecompiledCode = `package com.example;

public class User {
    private int id;
    private String name;
    private int age;
    
    public User() {
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
}`;

/**
 * 获取类列表
 * @returns {Promise<Object>} 类列表数据
 */
export async function getClassList() {
  try {
    // 使用模拟数据
    return mockClassTree;
  } catch (error) {
    console.error('获取类列表失败:', error);
    throw error;
  }
}

/**
 * 获取类详细信息
 * @param {string} className 类名
 * @returns {Promise<Object>} 类详细信息
 */
export async function getClassInfo(className) {
  try {
    // 使用模拟数据
    return mockClassAnalysis;
  } catch (error) {
    console.error('获取类详细信息失败:', error);
    throw error;
  }
}

/**
 * 获取反编译代码
 * @param {string} className 类名
 * @returns {Promise<string>} 反编译代码
 */
export async function getDecompiledCode(className) {
  try {
    // 使用模拟数据
    return mockDecompiledCode;
  } catch (error) {
    console.error('获取反编译代码失败:', error);
    return `// 获取反编译代码失败: ${error.message}\n// 类名: ${className}`;
  }
}

/**
 * 获取类树数据
 * @returns {Promise<Object>} 类树数据
 */
export async function getClassTree() {
  try {
    // 使用模拟数据
    return mockClassTree;
  } catch (error) {
    console.error('获取类树数据失败:', error);
    throw error;
  }
}

/**
 * 获取类分析信息
 * @param {string} className 类名
 * @returns {Promise<Object>} 类分析信息
 */
export async function getClassAnalysis(className) {
  try {
    // 使用模拟数据
    return mockClassAnalysis;
  } catch (error) {
    console.error('获取类分析信息失败:', error);
    throw error;
  }
}

/**
 * 为方法注入日志代码
 * @param {Object} injectionData 注入数据
 * @returns {Promise<Object>} 注入结果
 */
export async function injectMethodLog(injectionData) {
  try {
    // 使用模拟数据
    return { success: true, message: "注入成功" };
  } catch (error) {
    console.error('方法注入失败:', error);
    throw error;
  }
}